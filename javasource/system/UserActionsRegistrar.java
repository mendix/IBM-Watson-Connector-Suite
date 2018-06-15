package system;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.mendix.core.actionmanagement.IActionRegistrator;

@Component(immediate = true)
public class UserActionsRegistrar
{
  @Reference
  public void registerActions(IActionRegistrator registrator)
  {
    registrator.bundleComponentLoaded();
    registrator.registerUserAction(appcloudservices.actions.GenerateRandomPassword.class);
    registrator.registerUserAction(appcloudservices.actions.LogOutUser.class);
    registrator.registerUserAction(appcloudservices.actions.StartSignOnServlet.class);
    registrator.registerUserAction(cfcommons.actions.getEnvVariables.class);
    registrator.registerUserAction(system.actions.VerifyPassword.class);
    registrator.registerUserAction(watsonservices.actions.AnalyzeGeneralTone.class);
    registrator.registerUserAction(watsonservices.actions.ClassifyImage.class);
    registrator.registerUserAction(watsonservices.actions.CreateClassifier.class);
    registrator.registerUserAction(watsonservices.actions.DetectFaces.class);
    registrator.registerUserAction(watsonservices.actions.GetIdentifiableLanguages.class);
    registrator.registerUserAction(watsonservices.actions.RecongnizeAudio.class);
    registrator.registerUserAction(watsonservices.actions.SendMessage.class);
    registrator.registerUserAction(watsonservices.actions.Synthesize.class);
    registrator.registerUserAction(watsonservices.actions.Translate.class);
  }
}
