package com.aptana.deploy.redhat.ui.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.aptana.deploy.DeployPlugin;
import com.aptana.deploy.preferences.DeployPreferenceUtil;
import com.aptana.deploy.preferences.IPreferenceConstants.DeployType;
import com.aptana.deploy.redhat.RedHatAPI;
import com.aptana.deploy.wizard.IDeployWizard;

public class RedHatDeployWizard extends Wizard implements IDeployWizard
{

	private IProject project;

	public RedHatDeployWizard()
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public void addPages()
	{
		super.addPages();

		RedHatAPI api = new RedHatAPI();
		IStatus status = api.authenticate();
		if (status.isOK())
		{
			addPage(new RedHatDeployWizardPage());
		}
		else
		{
			addPage(new RedHatSignupWizardPage());
			// FIXME Do I need to do this?
			addPage(new RedHatDeployWizardPage());
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
		RedHatDeployWizardPage page = (RedHatDeployWizardPage) currentPage;
		IRunnableWithProgress runnable = createRedHatDeployRunnable(page);
		DeployType type = DeployType.RED_HAT;

		if (type != null)
		{
			DeployPreferenceUtil.setDeployType(project, type);
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

	protected IRunnableWithProgress createRedHatDeployRunnable(RedHatDeployWizardPage page)
	{
		IRunnableWithProgress runnable;
		final String appname = page.getAppName();
		final String type = page.getType();
		final IPath destination = page.getDestination();
		runnable = new IRunnableWithProgress()
		{

			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable()
				{

					public void run()
					{
						RedHatAPI api = new RedHatAPI();
						api.createApp(appname, type, destination);
					}
				});
			}

		};
		return runnable;
	}
}
