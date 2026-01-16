using Android.App;
using Android.Content;
using Java.Lang;
using WoWonder.Helpers.Model;
using WoWonder.Helpers.Utils;

namespace WoWonder.Services
{
    [BroadcastReceiver(Exported = true)]
    [IntentFilter(new[] { Intent.ActionBootCompleted })]
    public class AppApiReceiver : BroadcastReceiver
    {
        public override void OnReceive(Context context, Intent intent)
        {
            try
            {
                if (intent.Action.Equals("android.intent.action.BOOT_COMPLETED") && !string.IsNullOrEmpty(UserDetails.AccessToken))
                {
                    //here we start the service  again.           
                    AppApiService.GetInstance()?.StartForegroundService(context);
                }
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }
    }
}
