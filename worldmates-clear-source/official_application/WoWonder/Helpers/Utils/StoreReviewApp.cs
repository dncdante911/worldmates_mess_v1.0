using Android.Content;
using Android.OS;
using Exception = System.Exception;
using Uri = Android.Net.Uri;

namespace WoWonder.Helpers.Utils
{
    public class StoreReviewApp  
    {
        private Intent GetRateIntent(string url)
        {
            try
            {
                var intent = new Intent(Intent.ActionView, Uri.Parse(url));

                intent.AddFlags(ActivityFlags.NoHistory);
                intent.AddFlags(ActivityFlags.MultipleTask);
                if ((int)Build.VERSION.SdkInt >= 21)
                {
                    intent.AddFlags(ActivityFlags.NewDocument);
                }
                else
                {
                    intent.AddFlags(ActivityFlags.ClearWhenTaskReset);
                }
                intent.SetFlags(ActivityFlags.ClearTop);
                intent.SetFlags(ActivityFlags.NewTask);
                return intent;
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
                return null;
            }
        }

        /// <summary>
        /// Opens the store review page.
        /// </summary>
        /// <param name="appId">App identifier.</param>
        public void OpenStoreReviewPage(Context context, string appId)
        {
            //try
            //{
            //    var manager = ReviewManagerFactory.Create(context);
            //    var request = manager.RequestReviewFlow();
            //    request.AddOnCompleteListener(this);
            //    request.AddOnFailureListener(this);
            //    return;
            //}
            //catch (Exception ex)
            //{
            //    //Unable to launch app store
            //    Methods.DisplayReportResultTrack(ex);
            //}

            //Unable to launch app store
            var url = $"market://details?id={appId}";
            try
            {
                var intent = GetRateIntent(url);
                context.StartActivity(intent);
                return;
            }
            catch (Exception ex)
            {
                //Unable to launch app store
                Methods.DisplayReportResultTrack(ex);
            }

            url = $"https://play.google.com/store/apps/details?id={appId}";
            try
            {
                var intent = GetRateIntent(url);
                context.StartActivity(intent);
            }
            catch (Exception ex)
            {
                //Unable to launch app store:
                Methods.DisplayReportResultTrack(ex);
            }
        } 
    }
}