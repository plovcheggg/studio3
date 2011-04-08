package com.aptana.deploy.heroku.ui.wizard;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.internal.browser.BrowserViewer;
import org.eclipse.ui.internal.browser.WebBrowserEditor;
import org.eclipse.ui.internal.browser.WebBrowserEditorInput;
import org.osgi.service.prefs.BackingStoreException;

import com.aptana.core.util.IOUtil;
import com.aptana.deploy.DeployPlugin;
import com.aptana.deploy.heroku.HerokuAPI;
import com.aptana.deploy.preferences.DeployPreferenceUtil;
import com.aptana.deploy.preferences.IPreferenceConstants;
import com.aptana.deploy.preferences.IPreferenceConstants.DeployType;
import com.aptana.deploy.wizard.IDeployWizard;
import com.aptana.deploy.wizard.Messages;
import com.aptana.git.core.GitPlugin;
import com.aptana.git.core.model.GitRepository;
import com.aptana.git.core.model.IGitRepositoryManager;
import com.aptana.scripting.model.BundleElement;
import com.aptana.scripting.model.BundleEntry;
import com.aptana.scripting.model.BundleManager;
import com.aptana.scripting.model.CommandContext;
import com.aptana.scripting.model.CommandElement;
import com.aptana.usage.PingStartup;

public class HerokuDeployWizard extends Wizard implements IDeployWizard
{

	private static final String BUNDLE_HEROKU = "Heroku"; //$NON-NLS-1$

	private IProject project;

	@Override
	public void addPages()
	{
		super.addPages();

		File credentials = HerokuAPI.getCredentialsFile();
		if (credentials.exists() && HerokuAPI.fromCredentials().authenticate().isOK())
		{
			addPage(new HerokuDeployWizardPage());
		}
		else
		{
			addPage(new HerokuLoginWizardPage());
		}
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		Object element = selection.getFirstElement();
		if (element instanceof IResource)
		{
			IResource resource = (IResource) element;
			this.project = resource.getProject();
		}
	}

	@Override
	public boolean performFinish()
	{
		IWizardPage currentPage = getContainer().getCurrentPage();
		String pageName = currentPage.getName();
		DeployType type = null;
		String deployEndpointName = null;
		IRunnableWithProgress runnable = null;
		if (HerokuDeployWizardPage.NAME.equals(pageName))
		{
			HerokuDeployWizardPage page = (HerokuDeployWizardPage) currentPage;
			runnable = createHerokuDeployRunnable(page);
			type = DeployType.HEROKU;
			deployEndpointName = page.getAppName();
		}
		else if (HerokuSignupPage.NAME.equals(pageName))
		{
			HerokuSignupPage page = (HerokuSignupPage) currentPage;
			runnable = createHerokuSignupRunnable(page);
		}

		if (type != null)
		{
			DeployPreferenceUtil.setDeployType(project, type);
			if (deployEndpointName != null)
			{
				DeployPreferenceUtil.setDeployEndpoint(project, deployEndpointName);
			}
		}

		if (runnable != null)
		{
			try
			{
				getContainer().run(true, false, runnable);
			}
			catch (Exception e)
			{
				DeployPlugin.logError(e);
			}
		}
		return true;
	}

	protected IRunnableWithProgress createHerokuDeployRunnable(HerokuDeployWizardPage page)
	{
		IRunnableWithProgress runnable;
		final String appName = page.getAppName();
		final boolean publishImmediately = page.publishImmediately();

		// persists the auto-publish setting
		IEclipsePreferences prefs = (new InstanceScope()).getNode(DeployPlugin.getPluginIdentifier());
		prefs.putBoolean(IPreferenceConstants.HEROKU_AUTO_PUBLISH, publishImmediately);
		try
		{
			prefs.flush();
		}
		catch (BackingStoreException e)
		{
		}

		runnable = new IRunnableWithProgress()
		{

			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				SubMonitor sub = SubMonitor.convert(monitor, 100);
				try
				{
					// Initialize git repo for project if necessary
					IGitRepositoryManager manager = GitPlugin.getDefault().getGitRepositoryManager();
					GitRepository repo = manager.createOrAttach(project, sub.newChild(20));
					// TODO What if we didn't create the repo right now, but it is "dirty"?
					// Now do an initial commit
					repo.index().refresh(sub.newChild(15));
					repo.index().stageFiles(repo.index().changedFiles());
					repo.index().commit(Messages.DeployWizard_AutomaticGitCommitMessage);
					sub.worked(10);

					// Run commands to create/deploy
					PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable()
					{

						public void run()
						{
							CommandElement command;
							if (publishImmediately)
							{
								command = getCommand(BUNDLE_HEROKU, "Create and Deploy App"); //$NON-NLS-1$
							}
							else
							{
								command = getCommand(BUNDLE_HEROKU, "Create App"); //$NON-NLS-1$
							}
							// TODO What if command is null!?
							if (command != null)
							{
								// Send along the app name
								CommandContext context = command.createCommandContext();
								context.put("HEROKU_APP_NAME", appName); //$NON-NLS-1$
								command.execute(context);
							}
						}
					});
				}
				catch (CoreException ce)
				{
					throw new InvocationTargetException(ce);
				}
				finally
				{
					sub.done();
				}
			}

		};
		return runnable;
	}

	protected IRunnableWithProgress createHerokuSignupRunnable(HerokuSignupPage page)
	{
		IRunnableWithProgress runnable;
		final String userID = page.getUserID();
		runnable = new IRunnableWithProgress()
		{

			/**
			 * Send a ping to aptana.com with email address for referral tracking
			 * 
			 * @throws IOException
			 */
			private String sendPing(IProgressMonitor monitor) throws IOException
			{
				HttpURLConnection connection = null;
				try
				{
					final String HOST = "http://toolbox.aptana.com"; //$NON-NLS-1$
					StringBuilder builder = new StringBuilder(HOST);
					builder.append("/webhook/heroku?request_id="); //$NON-NLS-1$
					builder.append(URLEncoder.encode(PingStartup.getApplicationId(), "UTF-8")); //$NON-NLS-1$
					builder.append("&email="); //$NON-NLS-1$
					builder.append(URLEncoder.encode(userID, "UTF-8")); //$NON-NLS-1$
					builder.append("&type=signuphook"); //$NON-NLS-1$

					URL url = new URL(builder.toString());
					connection = (HttpURLConnection) url.openConnection();
					connection.setUseCaches(false);
					connection.setAllowUserInteraction(false);
					int responseCode = connection.getResponseCode();
					if (responseCode != HttpURLConnection.HTTP_OK)
					{
						// Log an error
						DeployPlugin.logError(
								MessageFormat.format(Messages.DeployWizard_FailureToGrabHerokuSignupJSError,
										builder.toString()), null);
					}
					else
					{
						return IOUtil.read(connection.getInputStream());
					}
				}
				finally
				{
					if (connection != null)
					{
						connection.disconnect();
					}
				}
				return ""; //$NON-NLS-1$
			}

			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				SubMonitor sub = SubMonitor.convert(monitor, 100);
				try
				{
					String javascriptToInject = sendPing(sub.newChild(40));
					openSignup(javascriptToInject, sub.newChild(60));
				}
				catch (Exception e)
				{
					throw new InvocationTargetException(e);
				}
				finally
				{
					sub.done();
				}
			}

			/**
			 * Open the Heroku signup page.
			 * 
			 * @param monitor
			 * @throws Exception
			 */
			private void openSignup(final String javascript, IProgressMonitor monitor) throws Exception
			{
				final String BROWSER_ID = "heroku-signup"; //$NON-NLS-1$
				final URL url = new URL("https://api.heroku.com/signup/aptana3"); //$NON-NLS-1$

				final int style = IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.LOCATION_BAR
						| IWorkbenchBrowserSupport.STATUS;
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
				{

					public void run()
					{
						openSignupURLinEclipseBrowser(url, style, BROWSER_ID, javascript);
					}
				});
			}
		};
		return runnable;
	}

	private CommandElement getCommand(String bundleName, String commandName)
	{
		BundleEntry entry = BundleManager.getInstance().getBundleEntry(bundleName);
		if (entry == null)
		{
			return null;
		}
		for (BundleElement bundle : entry.getContributingBundles())
		{
			CommandElement command = bundle.getCommandByName(commandName);
			if (command != null)
			{
				return command;
			}
		}
		return null;
	}

	@SuppressWarnings("restriction")
	private void openSignupURLinEclipseBrowser(URL url, int style, String browserId, final String javascript)
	{
		try
		{
			WebBrowserEditorInput input = new WebBrowserEditorInput(url, style, browserId);
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IEditorPart editorPart = page.openEditor(input, WebBrowserEditor.WEB_BROWSER_EDITOR_ID);
			WebBrowserEditor webBrowserEditor = (WebBrowserEditor) editorPart;
			Field f = WebBrowserEditor.class.getDeclaredField("webBrowser"); //$NON-NLS-1$
			f.setAccessible(true);
			BrowserViewer viewer = (BrowserViewer) f.get(webBrowserEditor);
			final Browser browser = viewer.getBrowser();
			browser.addProgressListener(new ProgressListener()
			{

				public void completed(ProgressEvent event)
				{
					browser.removeProgressListener(this);
					browser.execute(javascript);
				}

				public void changed(ProgressEvent event)
				{
					// ignore
				}
			});
		}
		catch (Exception e)
		{
			DeployPlugin.logError(e);
		}
	}
}
