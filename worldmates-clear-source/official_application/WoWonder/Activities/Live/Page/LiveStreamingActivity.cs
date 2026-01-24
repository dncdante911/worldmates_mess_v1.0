using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Globalization;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Threading.Tasks;
using System.Timers;
using Android.App;
using Android.Content;
using Android.Content.PM;
using Android.Graphics;
using Android.OS;
using Android.Views;
using Android.Widget;
using AndroidX.AppCompat.Widget;
using AndroidX.ConstraintLayout.Widget;
using AndroidX.RecyclerView.Widget;
using Bumptech.Glide.Util;
using Com.Aghajari.Emojiview.View;
using DE.Hdodenhof.CircleImageViewLib;
using Google.Android.Material.Dialog;
using IO.Agora.Rtc2;
using IO.Agora.Rtc2.Video;
using Java.Lang;
using Newtonsoft.Json;
using WoWonder.Activities.Base;
using WoWonder.Activities.Comment.Adapters;
using WoWonder.Activities.Live.Adapters;
using WoWonder.Activities.Live.Filters;
using WoWonder.Activities.Live.Rtc;
using WoWonder.Activities.Live.Stats;
using WoWonder.Activities.Live.Ui;
using WoWonder.Activities.NativePost.Post;
using WoWonder.Activities.NativePost.Share;
using WoWonder.Activities.Tabbes;
using WoWonder.Helpers.CacheLoaders;
using WoWonder.Helpers.Controller;
using WoWonder.Helpers.Fonts;
using WoWonder.Helpers.Model;
using WoWonder.Helpers.Utils;
using WoWonder.Library.Anjo.IntegrationRecyclerView;
using WoWonder.StickersView;
using WoWonderClient;
using WoWonderClient.Classes.Comments;
using WoWonderClient.Classes.Global;
using WoWonderClient.Classes.Posts;
using WoWonderClient.Requests;
using Encoding = System.Text.Encoding;
using Exception = System.Exception;
using Object = Java.Lang.Object;
using Uri = Android.Net.Uri;

namespace WoWonder.Activities.Live.Page
{
    [Activity(Icon = "@mipmap/icon", Theme = "@style/MyTheme", ConfigurationChanges = ConfigChanges.Locale | ConfigChanges.UiMode | ConfigChanges.ScreenSize | ConfigChanges.Orientation | ConfigChanges.ScreenLayout | ConfigChanges.SmallestScreenSize)]
    public class LiveStreamingActivity : RtcBaseActivity, IEventHandler, IDialogListCallBack/*, PixelCopy.IOnPixelCopyFinishedListener*/
    {
        #region Variables Basic

        private FrameLayout RootView, MVideoControlLyt, MLiveStreamEndedLyt;
        private ViewStub MHeaderViewStub, MFooterViewStub;
        private ConstraintLayout ContentView;
        private ConstraintLayout MGetReadyLyt, MLoadingViewer;
        private ImageView MAvatarBg, MCloseIn, MCloseOut, MCloseStreaming;
        private CircleImageView MAvatar;
        private TextView MTimeText, MViewersText;

        ////////////// Comments ///////////// 
        private ImageView MEmojisIconBtn, MMoreBtn, MShareBtn;
        private TextView MCameraBtn, MEffectBtn, MVideoEnabledBtn, MAudioEnabledBtn, MaskBtn, BackgroundBtn;
        private AXEmojiEditText TxtComment;
        private ImageView MSendBtn;
        private LinearLayoutManager LayoutManager;
        private RecyclerView MRecycler;
        private LiveMessageAdapter MAdapter;
        private Timer TimerComments;

        ////////////////////////////////
        private PostDataObject LiveStreamViewerObject;
        private PostDataObject PostObject;
        private string PostId, MStreamChannel;
        private bool IsOwner, IsStreamingStarted;
        private int Role;

        //////////////////////////////// 
        private VideoGridContainer MVideoLayout;
        private SurfaceView SurfaceView;
        private VideoEncoderConfiguration.VideoDimensions MVideoDimension;

        ////////////////////////////////
        private ImageView BgAvatar, CloseEnded;
        private CircleImageView StreamRateLevel;
        private TextView Header, ShareStreamText, Comments, Viewers, Duration;
        private AppCompatButton GoLiveButton;
        private LinearLayout InfoLiveLayout;

        //////////////////////////////// 
        private bool IsStreamingTimeInitialed;
        private Handler CustomHandler;
        private MyRunnable UpdateTimerThread;
        private long StartTime;
        private long TimeInMilliseconds;
        private long TimeSwapBuff;
        private long UpdatedTime;

        ////////////////////////////////
        private string UidLive, ResourceId, SId, FileListLive;

        #endregion

        #region General

        protected override void OnCreate(Bundle savedInstanceState)
        {
            try
            {
                MStreamChannel = Intent?.GetStringExtra("StreamName") ?? "";
                Config().SetChannelName(MStreamChannel);

                base.OnCreate(savedInstanceState);

                // Create your application here
                SetContentView(Resource.Layout.LiveStreamingLayout);

                PostId = Intent?.GetStringExtra("PostId") ?? "";
                var audience = Constants.ClientRoleAudience;
                Role = Intent?.GetIntExtra(LiveConstants.KeyClientRole, audience) ?? audience;
                IsOwner = Role == Constants.ClientRoleBroadcaster;  //Owner >> ClientRoleBroadcaster , Users >> ClientRoleAudience

                if (!IsOwner)
                    LiveStreamViewerObject = JsonConvert.DeserializeObject<PostDataObject>(Intent?.GetStringExtra("PostLiveStream") ?? "");

                //Get Value And Set Toolbar
                InitComponent();
                SetRecyclerViewAdapters();
                InitBackPressed();
                InitAgora();
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        protected override void OnResume()
        {
            try
            {
                base.OnResume();
                AddOrRemoveEvent(true);
                StartTimerComment();
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        protected override void OnPause()
        {
            try
            {
                base.OnPause();
                AddOrRemoveEvent(false);
                StopTimerComment();
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public override void OnTrimMemory(TrimMemory level)
        {
            try
            {
                base.OnTrimMemory(level);
                GC.Collect(GC.MaxGeneration, GCCollectionMode.Forced);
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public override void OnLowMemory()
        {
            try
            {
                base.OnLowMemory();
                GC.Collect(GC.MaxGeneration);
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        protected override void OnDestroy()
        {
            try
            {
                DestroyBasic();
                base.OnDestroy();
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        #endregion

        #region Menu

        public override bool OnOptionsItemSelected(IMenuItem item)
        {
            if (item.ItemId == Android.Resource.Id.Home)
            {
                BackPressed();
                return true;
            }

            return base.OnOptionsItemSelected(item);
        }

        #endregion

        #region BackPressed && Close Live

        public void BackPressed()
        {
            try
            {
                if (IsOwner && IsStreamingStarted)
                {
                    SetupFinishAsk(true);
                }
                else if (IsOwner)
                {
                    DeleteLiveStream();
                    Finish();
                }
                else
                {
                    if (IsStreamingStarted)
                        SetupFinishAsk(false);
                    else
                        Finish();
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void SetupFinishAsk(bool isStreamer)
        {
            try
            {
                var dialog = new MaterialAlertDialogBuilder(this);
                if (isStreamer)
                {
                    dialog.SetTitle(Resource.String.Lbl_LiveStreamer_alert_title);
                    dialog.SetMessage(GetText(Resource.String.Lbl_LiveStreamer_alert_message));
                }
                else
                {
                    dialog.SetTitle(Resource.String.Lbl_LiveViewer_alert_title);
                    dialog.SetMessage(GetText(Resource.String.Lbl_LiveViewer_alert_message));
                }

                dialog.SetPositiveButton(GetText(Resource.String.Lbl_Yes), (materialDialog, action) =>
                {
                    try
                    {
                        FinishStreaming(isStreamer);
                    }
                    catch (Exception e)
                    {
                        Methods.DisplayReportResultTrack(e);
                    }
                });
                dialog.SetNegativeButton(GetText(Resource.String.Lbl_No), new MaterialDialogUtils());

                dialog.Show();
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private async void FinishStreaming(bool isStreamer)
        {
            try
            {
                IsStreamingStarted = false;

                TabbedMainActivity.GetInstance()?.SetOffWakeLock();

                StopTimer();
                DestroyTimerComment();

                if (IsOwner)
                {
                    if (isStreamer)
                    {
                        if (ListUtils.SettingsSiteList?.LiveVideoSave is "0")
                            DeleteLiveStream();
                        else
                        {
                            await AgoraStop(AppSettings.AppIdAgoraLive, ListUtils.SettingsSiteList?.AgoraCustomerId, ListUtils.SettingsSiteList?.AgoraCustomerCertificate, ResourceId, SId, Config().GetChannelName(), UidLive);
                        }
                    }
                    else
                        DeleteLiveStream();
                }

                StatsManager()?.ClearAllData();
                RemoveEventHandler(this);
                StopRtc();
                //add end page
                LiveStreamEnded();
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void LiveStreamEnded()
        {
            try
            {
                MLiveStreamEndedLyt.Visibility = ViewStates.Visible;

                MLoadingViewer.Visibility = ViewStates.Gone;
                MGetReadyLyt.Visibility = ViewStates.Gone;

                MVideoControlLyt.Visibility = ViewStates.Gone;
                MVideoLayout.Visibility = ViewStates.Gone;

                BgAvatar = FindViewById<ImageView>(Resource.Id.bg_avatar_end);
                StreamRateLevel = FindViewById<CircleImageView>(Resource.Id.streamRateLevel);
                CloseEnded = FindViewById<ImageView>(Resource.Id.close_ended);
                CloseEnded.Click += CloseEndedOnClick;

                Header = FindViewById<TextView>(Resource.Id.header);
                ShareStreamText = FindViewById<TextView>(Resource.Id.shareStreamText);

                GoLiveButton = FindViewById<AppCompatButton>(Resource.Id.goLiveButton);

                InfoLiveLayout = FindViewById<LinearLayout>(Resource.Id.infoLiveLayout);

                Comments = FindViewById<TextView>(Resource.Id.commentsValue);
                Viewers = FindViewById<TextView>(Resource.Id.viewersValue);
                Duration = FindViewById<TextView>(Resource.Id.timeValue);


                if (IsOwner)
                {
                    if (PostObject != null)
                    {
                        GlideImageLoader.LoadImage(this, PostObject.Publisher.Avatar, BgAvatar, ImageStyle.CenterCrop,
                            ImagePlaceholders.Drawable);
                        StreamRateLevel.SetImageURI(Uri.Parse(PostObject.Publisher.Avatar));
                        //GlideImageLoader.LoadImage(this, PostObject.Avater, StreamRateLevel, ImageStyle.CircleCrop, ImagePlaceholders.Drawable);
                    }

                    Header.Text = GetText(Resource.String.Lbl_YourLiveStreamHasEnded);
                    ShareStreamText.Text = GetText(Resource.String.Lbl_LiveStreamer_End_title);

                    InfoLiveLayout.Visibility = ViewStates.Visible;
                    GoLiveButton.Visibility = ViewStates.Gone;

                    Comments.Text = MAdapter.CommentList.Count.ToString();
                    Viewers.Text = MViewersText.Text?.Replace(GetText(Resource.String.Lbl_Views), "");
                    Duration.Text = MTimeText.Text;
                }
                else
                {
                    if (LiveStreamViewerObject != null)
                    {
                        GlideImageLoader.LoadImage(this, LiveStreamViewerObject.Publisher.Avatar, BgAvatar,
                            ImageStyle.CenterCrop, ImagePlaceholders.Drawable);
                        StreamRateLevel.SetImageURI(Uri.Parse(LiveStreamViewerObject.Publisher.Avatar));
                        //GlideImageLoader.LoadImage(this, LiveStreamViewerObject.Avater, StreamRateLevel, ImageStyle.CircleCrop, ImagePlaceholders.Drawable);
                    }

                    Header.Text = GetText(Resource.String.Lbl_LiveStreamHasEnded);
                    ShareStreamText.Text = GetText(Resource.String.Lbl_LiveViewer_End_title);

                    InfoLiveLayout.Visibility = ViewStates.Gone;
                    GoLiveButton.Visibility = ViewStates.Visible;
                    GoLiveButton.Click += GoLiveButtonOnClick;
                }
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void CloseEndedOnClick(object sender, EventArgs e)
        {
            try
            {
                Finish();
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void GoLiveButtonOnClick(object sender, EventArgs e)
        {
            try
            {
                var streamName = "live" + Methods.Time.CurrentTimeMillis();
                if (string.IsNullOrEmpty(streamName) || string.IsNullOrWhiteSpace(streamName))
                {
                    ToastUtils.ShowToast(this, GetText(Resource.String.Lbl_PleaseEnterLiveStreamName), ToastLength.Short);
                    return;
                }
                //Owner >> ClientRoleBroadcaster , Users >> ClientRoleAudience
                Intent intent = new Intent(this, typeof(LiveStreamingActivity));
                intent.PutExtra(LiveConstants.KeyClientRole, Constants.ClientRoleBroadcaster);
                intent.PutExtra("StreamName", streamName);
                StartActivity(intent);

                Finish();
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        #endregion

        #region Functions

        private void InitComponent()
        {
            try
            {
                RootView = FindViewById<FrameLayout>(Resource.Id.rootView);
                MHeaderViewStub = FindViewById<ViewStub>(Resource.Id.liveStreaming_headerStub);
                MFooterViewStub = FindViewById<ViewStub>(Resource.Id.liveStreaming_footer);

                ContentView = FindViewById<ConstraintLayout>(Resource.Id.contentView);

                MVideoControlLyt = FindViewById<FrameLayout>(Resource.Id.liveStreaming_videoAndControlsContainer);
                MGetReadyLyt = FindViewById<ConstraintLayout>(Resource.Id.streamerReady_root);
                MLoadingViewer = FindViewById<ConstraintLayout>(Resource.Id.loading_joining);
                MLiveStreamEndedLyt = FindViewById<FrameLayout>(Resource.Id.streamer_final_screen_root);

                MVideoLayout = FindViewById<VideoGridContainer>(Resource.Id.liveStreaming_videoContainer);
                MVideoLayout.SetStatsManager(StatsManager());

                MAvatarBg = FindViewById<ImageView>(Resource.Id.streamLoadingProgress_backgroundAvatar);
                MAvatar = FindViewById<CircleImageView>(Resource.Id.streamLoadingProgress_foregroundAvatar);

                MEmojisIconBtn = FindViewById<ImageView>(Resource.Id.sendEmojisIconButton);
                MMoreBtn = FindViewById<ImageView>(Resource.Id.more_btn);
                MShareBtn = FindViewById<ImageView>(Resource.Id.share_btn);
                TxtComment = FindViewById<AXEmojiEditText>(Resource.Id.MessageWrapper);
                MSendBtn = FindViewById<ImageView>(Resource.Id.sendMessageButton);

                InitEmojisView();

                if (IsOwner)
                {
                    MHeaderViewStub.LayoutResource = Resource.Layout.view_live_streaming_streamer_header;
                    MHeaderViewStub.Inflate();

                    MEmojisIconBtn.Visibility = ViewStates.Gone;
                    MMoreBtn.Visibility = ViewStates.Gone;

                    MFooterViewStub.LayoutResource = Resource.Layout.view_live_streaming_streamer_footer;
                    MFooterViewStub.Inflate();

                    InitViewerFooter();
                }
                else
                {
                    MHeaderViewStub.LayoutResource = Resource.Layout.view_live_streaming_viewer_header;
                    MHeaderViewStub.Inflate();

                    MEmojisIconBtn.Visibility = ViewStates.Visible;
                    MMoreBtn.Visibility = ViewStates.Visible;
                }

                MRecycler = FindViewById<RecyclerView>(Resource.Id.liveStreaming_messageList);

                MViewersText = FindViewById<TextView>(Resource.Id.livestreamingHeader_viewers);
                MCloseIn = FindViewById<ImageView>(Resource.Id.close_in);
                MCloseOut = FindViewById<ImageView>(Resource.Id.close_out);
                MCloseStreaming = FindViewById<ImageView>(Resource.Id.livestreamingHeader_close);

                MTimeText = FindViewById<TextView>(Resource.Id.livestreamingHeader_status);
                MViewersText.Text = "0 " + GetText(Resource.String.Lbl_Views);

                InitFilters();
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void InitEmojisView()
        {
            Methods.SetColorEditText(TxtComment, WoWonderTools.IsTabDark() ? Color.White : Color.Black);
            Task.Factory.StartNew(() =>
            {
                try
                {
                    if (WoWonderTools.IsTabDark())
                        EmojisViewTools.LoadDarkTheme();
                    else
                        EmojisViewTools.LoadTheme(AppSettings.MainColor);

                    EmojisViewTools.MStickerView = false;
                    EmojisViewTools.LoadView(this, TxtComment, "", MEmojisIconBtn);
                }
                catch (Exception e)
                {
                    Methods.DisplayReportResultTrack(e);
                }
            });
        }

        private void InitBackPressed()
        {
            try
            {
                if (Build.VERSION.SdkInt >= BuildVersionCodes.Tiramisu)
                {
                    OnBackInvokedDispatcher.RegisterOnBackInvokedCallback(0, new BackCallAppBase2(this, "LiveStreamingActivity"));
                }
                else
                {
                    OnBackPressedDispatcher.AddCallback(new BackCallAppBase1(this, "LiveStreamingActivity", true));
                }
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void SetRecyclerViewAdapters()
        {
            try
            {
                MAdapter = new LiveMessageAdapter(this)
                {
                    CommentList = new ObservableCollection<CommentObjectExtra>()
                };
                LayoutManager = new LinearLayoutManager(this);
                MRecycler.SetLayoutManager(LayoutManager);
                MRecycler.HasFixedSize = true;
                MRecycler.SetItemViewCacheSize(50);
                MRecycler.GetLayoutManager().ItemPrefetchEnabled = true;
                var sizeProvider = new FixedPreloadSizeProvider(10, 10);
                var preLoader = new RecyclerViewPreloader<CommentObjectExtra>(this, MAdapter, sizeProvider, 10);
                MRecycler.AddOnScrollListener(preLoader);
                MRecycler.SetAdapter(MAdapter);
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void AddOrRemoveEvent(bool addEvent)
        {
            try
            {
                if (addEvent)
                    // true +=  // false -=
                {
                    MAdapter.ItemLongClick += MAdapterOnItemLongClick;
                    if (MCloseIn != null) MCloseIn.Click += MCloseInOnClick;
                    if (MCloseOut != null) MCloseOut.Click += MCloseInOnClick;
                    if (MCloseStreaming != null) MCloseStreaming.Click += MCloseInOnClick;
                    if (MSendBtn != null) MSendBtn.Click += MSendBtnOnClick;
                    if (MMoreBtn != null) MMoreBtn.Click += MMoreBtnOnClick;
                    if (MShareBtn != null) MShareBtn.Click += MShareBtnOnClick;
                    if (MCameraBtn != null) MCameraBtn.Click += MCameraBtnOnClick;
                    if (MEffectBtn != null) MEffectBtn.Click += MEffectBtnOnClick;
                    if (MVideoEnabledBtn != null) MVideoEnabledBtn.Click += MVideoEnabledBtnOnClick;
                    if (MAudioEnabledBtn != null) MAudioEnabledBtn.Click += MAudioEnabledBtnOnClick;
                    if (MaskBtn != null) MaskBtn.Click += MaskBtnOnClick;
                    if (BackgroundBtn != null) BackgroundBtn.Click += BackgroundBtnOnClick;
                    if (MaskSeekBar != null) MaskSeekBar.ProgressChanged += MaskSeekBarOnProgressChanged;
                    if (MaskClose != null) MaskClose.Click += MaskCloseOnClick;
                }
                else
                {
                    MAdapter.ItemLongClick -= MAdapterOnItemLongClick;
                    if (MCloseIn != null) MCloseIn.Click -= MCloseInOnClick;
                    if (MCloseOut != null) MCloseOut.Click -= MCloseInOnClick;
                    if (MCloseStreaming != null) MCloseStreaming.Click -= MCloseInOnClick;
                    if (MSendBtn != null) MSendBtn.Click -= MSendBtnOnClick;
                    if (MMoreBtn != null) MMoreBtn.Click -= MMoreBtnOnClick;
                    if (MShareBtn != null) MShareBtn.Click -= MShareBtnOnClick;
                    if (MCameraBtn != null) MCameraBtn.Click -= MCameraBtnOnClick;
                    if (MEffectBtn != null) MEffectBtn.Click -= MEffectBtnOnClick;
                    if (MVideoEnabledBtn != null) MVideoEnabledBtn.Click -= MVideoEnabledBtnOnClick;
                    if (MAudioEnabledBtn != null) MAudioEnabledBtn.Click -= MAudioEnabledBtnOnClick;
                    if (MaskBtn != null) MaskBtn.Click -= MaskBtnOnClick;
                    if (BackgroundBtn != null) BackgroundBtn.Click -= BackgroundBtnOnClick;
                    if (MaskSeekBar != null) MaskSeekBar.ProgressChanged -= MaskSeekBarOnProgressChanged;
                    if (MaskClose != null) MaskClose.Click -= MaskCloseOnClick;
                }
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void DestroyBasic()
        {
            try
            {
                MVideoControlLyt = null!;
                MHeaderViewStub = null!;
                MFooterViewStub = null!;
                MGetReadyLyt = null!;
                MLoadingViewer = null!;
                MAvatarBg = null!;
                MCloseIn = null!;
                MCloseOut = null!;
                MCloseStreaming = null!;
                MAvatar = null!;
                MTimeText = null!;
                MEmojisIconBtn = null!;
                MMoreBtn = null!;
                MCameraBtn = null!;
                MEffectBtn = null!;
                MVideoEnabledBtn = null!;
                MAudioEnabledBtn = null!;
                TxtComment = null!;
                MSendBtn = null!;
                LayoutManager = null!;
                MRecycler = null!;
                MAdapter = null!;
                TimerComments = null!;
                LiveStreamViewerObject = null!;
                PostObject = null!;
                PostId = null!;
                MStreamChannel = null!;
                MVideoLayout = null!;
                SurfaceView = null!;
                MVideoDimension = null!;
                CustomHandler = null!;
                UpdateTimerThread = null!;
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void InitViewerFooter()
        {
            try
            {
                MCameraBtn = FindViewById<TextView>(Resource.Id.camera_switch_btn);

                MEffectBtn = FindViewById<TextView>(Resource.Id.effect_btn);
                MEffectBtn.Activated = true;

                MVideoEnabledBtn = FindViewById<TextView>(Resource.Id.video_enabled_btn);
                MVideoEnabledBtn.Activated = true;

                MAudioEnabledBtn = FindViewById<TextView>(Resource.Id.audio_enabled_btn);
                MAudioEnabledBtn.Activated = true;

                MaskBtn = FindViewById<TextView>(Resource.Id.mask_btn);
                MaskBtn.Activated = true;

                BackgroundBtn = FindViewById<TextView>(Resource.Id.background_btn);
                BackgroundBtn.Activated = true;

                FontUtils.SetTextViewIcon(FontsIconFrameWork.FontAwesomeRegular, MCameraBtn, FontAwesomeIcon.Camera);
                FontUtils.SetTextViewIcon(FontsIconFrameWork.FontAwesomeRegular, MEffectBtn, FontAwesomeIcon.Magic);
                FontUtils.SetTextViewIcon(FontsIconFrameWork.FontAwesomeRegular, MVideoEnabledBtn, FontAwesomeIcon.Video);
                FontUtils.SetTextViewIcon(FontsIconFrameWork.FontAwesomeRegular, MAudioEnabledBtn, FontAwesomeIcon.MicrophoneAlt);
                FontUtils.SetTextViewIcon(FontsIconFrameWork.FontAwesomeSolid, MaskBtn, FontAwesomeIcon.Mask);
                FontUtils.SetTextViewIcon(FontsIconFrameWork.FontAwesomeRegular, BackgroundBtn, FontAwesomeIcon.ImagePortrait);

                if (!BackgroundRender.IsFeatureAvailable())
                    BackgroundBtn.Visibility = ViewStates.Gone;

               // if (!AppSettings.EnableAgoraFaceUnityExtension)
                    MaskBtn.Visibility = ViewStates.Gone;

            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        #endregion

        #region Events

        private void MAdapterOnItemLongClick(object sender, LiveMessageAdapterClickEventArgs e)
        {
            try
            {
                var item = MAdapter.CommentList.LastOrDefault();
                if (item?.Publisher != null)
                {
                    TxtComment.Text = "";
                    TxtComment.Text = "@" + item.Publisher.Username + " ";
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void BackgroundBtnOnClick(object sender, EventArgs e)
        {
            try
            {
                ShowMask("BackgroundRender");
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void MaskBtnOnClick(object sender, EventArgs e)
        {
            try
            {
                ShowMask("FuRender");
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void MAudioEnabledBtnOnClick(object sender, EventArgs e)
        {
            try
            {
                if (sender is View view)
                {
                    MRtcEngine?.MuteLocalAudioStream(view.Activated);
                    view.Activated = !view.Activated;

                    if (view.Activated)
                        FontUtils.SetTextViewIcon(FontsIconFrameWork.FontAwesomeRegular, MAudioEnabledBtn, FontAwesomeIcon.MicrophoneAlt);
                    else
                        FontUtils.SetTextViewIcon(FontsIconFrameWork.FontAwesomeRegular, MAudioEnabledBtn, FontAwesomeIcon.MicrophoneAltSlash);
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void MVideoEnabledBtnOnClick(object sender, EventArgs e)
        {
            try
            {
                if (sender is View view)
                {
                    if (view.Activated)
                        StopBroadcast();
                    else
                        StartBroadcast();

                    view.Activated = !view.Activated;

                    if (view.Activated)
                        FontUtils.SetTextViewIcon(FontsIconFrameWork.FontAwesomeRegular, MVideoEnabledBtn, FontAwesomeIcon.Video);
                    else
                        FontUtils.SetTextViewIcon(FontsIconFrameWork.FontAwesomeRegular, MVideoEnabledBtn, FontAwesomeIcon.VideoSlash);
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void MEffectBtnOnClick(object sender, EventArgs e)
        {
            try
            {
                ShowMask("AgoraRender");
                //if (sender is View view)
                //{
                //    view.Activated = !view.Activated;
                //    MRtcEngine?.SetBeautyEffectOptions(view.Activated, LiveConstants.DefaultBeautyOptions);
                    
                //    if (view.Activated)
                //        MEffectBtn.SetTextColor(Color.ParseColor(AppSettings.MainColor));
                //    else
                //        MEffectBtn.SetTextColor(Color.White);
                //}
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void MCameraBtnOnClick(object sender, EventArgs e)
        {
            try
            {
                MRtcEngine?.SwitchCamera();
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void MShareBtnOnClick(object sender, EventArgs e)
        {
            try
            {
                Bundle bundle = new Bundle();

                bundle.PutString("ItemData", IsOwner ? JsonConvert.SerializeObject(PostObject) : JsonConvert.SerializeObject(LiveStreamViewerObject));
                bundle.PutString("TypePost", JsonConvert.SerializeObject(PostModelType.AgoraLivePost));
                var searchFilter = new ShareBottomDialogFragment
                {
                    Arguments = bundle
                };
                searchFilter.Show(SupportFragmentManager, "ShareFilter");
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void MMoreBtnOnClick(object sender, EventArgs e)
        {
            try
            {
                var arrayAdapter = new List<string>();
                var dialogList = new MaterialAlertDialogBuilder(this);

                arrayAdapter.Add(GetText(Resource.String.Lbl_ViewProfile));
                arrayAdapter.Add(GetText(Resource.String.Lbl_Copy));

                if (!IsOwner)
                    arrayAdapter.Add(GetText(Resource.String.Lbl_Report));

                dialogList.SetItems(arrayAdapter.ToArray(), new MaterialDialogUtils(arrayAdapter, this));
                dialogList.SetNegativeButton(GetText(Resource.String.Lbl_Close), new MaterialDialogUtils());

                dialogList.Show();
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private async void MSendBtnOnClick(object sender, EventArgs e)
        {
            try
            {
                if (string.IsNullOrEmpty(TxtComment.Text) && string.IsNullOrWhiteSpace(TxtComment.Text))
                    return;

                if (Methods.CheckConnectivity())
                {
                    var dataUser = ListUtils.MyProfileList?.FirstOrDefault();
                    //Comment Code 

                    var unixTimestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
                    string time2 = unixTimestamp.ToString(CultureInfo.InvariantCulture);

                    CommentObjectExtra comment = new CommentObjectExtra
                    {
                        Id = unixTimestamp.ToString(),
                        PostId = PostId,
                        UserId = UserDetails.UserId,
                        Text = TxtComment.Text,
                        Time = time2,
                        CFile = "",
                        Record = "",
                        Publisher = dataUser,
                        Url = dataUser?.Url,
                        Fullurl = PostObject?.PostUrl,
                        Orginaltext = TxtComment.Text,
                        Owner = true,
                        CommentLikes = "0",
                        CommentWonders = "0",
                        IsCommentLiked = false,
                        Replies = "0",
                        RepliesCount = "0"
                    };

                    MAdapter.CommentList.Add(comment);

                    var index = MAdapter.CommentList.IndexOf(comment);
                    if (index > -1) MAdapter.NotifyItemInserted(index);

                    var text = TxtComment.Text;

                    //Hide keyboard
                    TxtComment.Text = "";

                    var (apiStatus, respond) = await RequestsAsync.Comment.CreatePostCommentsAsync(PostId, text, "", "", "");
                    if (apiStatus == 200)
                    {
                        if (respond is CreateComments result)
                        {
                            var date = MAdapter.CommentList.FirstOrDefault(a => a.Id == comment.Id) ?? MAdapter.CommentList.FirstOrDefault(x => x.Id == result.Data.Id);
                            if (date != null)
                            {
                                var db = ClassMapper.Mapper?.Map<CommentObjectExtra>(result.Data);

                                date = db;
                                date.Id = result.Data.Id;

                                index = MAdapter.CommentList.IndexOf(MAdapter.CommentList.FirstOrDefault(a => a.Id == unixTimestamp.ToString()));
                                if (index > -1)
                                {
                                    MAdapter.CommentList[index] = db;

                                    MAdapter.NotifyItemChanged(index);
                                    MRecycler.ScrollToPosition(index);
                                }

                                var postFeedAdapter = TabbedMainActivity.GetInstance()?.NewsFeedTab?.PostFeedAdapter;
                                var dataGlobal = postFeedAdapter?.ListDiffer?.Where(a => a.PostData?.Id == PostId).ToList();
                                if (dataGlobal?.Count > 0)
                                    foreach (var dataClass in from dataClass in dataGlobal let indexCom = postFeedAdapter.ListDiffer.IndexOf(dataClass) where indexCom > -1 select dataClass)
                                    {
                                        dataClass.PostData.PostComments = MAdapter.CommentList.Count.ToString();

                                        if (dataClass.PostData.GetPostComments?.Count > 0)
                                        {
                                            var dataComment = dataClass.PostData.GetPostComments.FirstOrDefault(a => a.Id == date.Id);
                                            if (dataComment == null) dataClass.PostData.GetPostComments.Add(date);
                                        }
                                        else
                                        {
                                            dataClass.PostData.GetPostComments = new List<CommentDataObject> { date };
                                        }

                                        postFeedAdapter?.NotifyItemChanged(postFeedAdapter.ListDiffer.IndexOf(dataClass), "commentReplies");
                                    }
                            }
                        }
                    }
                    //else Methods.DisplayReportResult(this, respond);

                    //Hide keyboard
                    TxtComment.Text = "";
                }
                else
                {
                    ToastUtils.ShowToast(this, GetText(Resource.String.Lbl_CheckYourInternetConnection), ToastLength.Short);
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void MCloseInOnClick(object sender, EventArgs e)
        {
            try
            {
                BackPressed();
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        #endregion

        #region Agora

        private void InitAgora()
        {
            try
            {
                if (IsOwner)
                    MRtcEngine?.SetClientRole(Constants.ClientRoleBroadcaster);
                else
                    MRtcEngine?.SetClientRole(Constants.ClientRoleAudience);

                RegisterEventHandler(this);
                
                MVideoDimension = LiveConstants.VideoDimensions[Config().GetVideoDimenIndex()];
                InitValueLive();
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void InitValueLive()
        {
            try
            {
                IsStreamingStarted = false;

                if (IsOwner)
                {
                    MGetReadyLyt.Visibility = ViewStates.Visible;
                    MLoadingViewer.Visibility = ViewStates.Gone;
                    MVideoControlLyt.Visibility = ViewStates.Gone;
                    MLiveStreamEndedLyt.Visibility = ViewStates.Gone;

                    CreateLiveStream();
                }
                else
                {
                    if (LiveStreamViewerObject != null)
                    {
                        GlideImageLoader.LoadImage(this, LiveStreamViewerObject.Publisher.Avatar, MAvatarBg, ImageStyle.CenterCrop, ImagePlaceholders.DrawableUser);
                        GlideImageLoader.LoadImage(this, LiveStreamViewerObject.Publisher.Avatar, MAvatar, ImageStyle.CircleCrop, ImagePlaceholders.DrawableUser);
                    }

                    MLoadingViewer.Visibility = ViewStates.Visible;
                    MGetReadyLyt.Visibility = ViewStates.Gone;
                    MVideoControlLyt.Visibility = ViewStates.Gone;
                    MLiveStreamEndedLyt.Visibility = ViewStates.Gone;

                    JoinChannel();
                }
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void InitStreamerInfo()
        {
            try
            {
                ImageView streamerAvatar = FindViewById<ImageView>(Resource.Id.livestreamingHeader_streamerImage);
                TextView streamerName = FindViewById<TextView>(Resource.Id.livestreamingHeader_name);

                if (LiveStreamViewerObject != null)
                {
                    GlideImageLoader.LoadImage(this, LiveStreamViewerObject.Publisher.Avatar, streamerAvatar, ImageStyle.CircleCrop, ImagePlaceholders.DrawableUser);
                    streamerName.Text = WoWonderTools.GetNameFinal(LiveStreamViewerObject.Publisher);

                    //if (LiveStreamViewerObject.LiveTime != null)
                    //    SetTimer(LiveStreamViewerObject.LiveTime.Value);
                    MTimeText.Text = GetText(Resource.String.Lbl_Live);
                }
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void StartBroadcast()
        {
            try
            {
                MRtcEngine?.SetClientRole(Constants.ClientRoleBroadcaster);
                SurfaceView = PrepareRtcVideo(0, true);
                MVideoLayout.AddUserVideotextureView(0, SurfaceView, true);

                MLoadingViewer.Visibility = ViewStates.Gone;
                MGetReadyLyt.Visibility = ViewStates.Gone;
                MLiveStreamEndedLyt.Visibility = ViewStates.Gone;

                MVideoControlLyt.Visibility = ViewStates.Visible;
                MVideoLayout.Visibility = ViewStates.Visible;
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void StopBroadcast()
        {
            try
            {
                MRtcEngine?.SetClientRole(Constants.ClientRoleAudience);
                RemoveRtcVideo(0, true);
                MVideoLayout.RemoveUserVideo(0, true);

                MLoadingViewer.Visibility = ViewStates.Gone;
                MGetReadyLyt.Visibility = ViewStates.Gone;
                MLiveStreamEndedLyt.Visibility = ViewStates.Gone;

                MVideoControlLyt.Visibility = ViewStates.Visible;
                MVideoLayout.Visibility = ViewStates.Visible;
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public void OnFirstLocalVideoFrame(Constants.VideoSourceType source, int width, int height, int elapsed)
        {
            try
            {
                if (!IsStreamingTimeInitialed)
                {
                    SetTimer(SystemClock.ElapsedRealtime());
                    IsStreamingTimeInitialed = true;
                }
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public void OnFirstRemoteVideoFrame(int uid, int width, int height, int elapsed)
        {
            try
            {
                RunOnUiThread(() =>
                {
                    RenderRemoteUser(uid);
                });
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public void OnLeaveChannel(IRtcEngineEventHandler.RtcStats stats)
        {
             
        }

        public void OnJoinChannelSuccess(string channel, int uid, int elapsed)
        {
            try
            {
                UidLive = uid.ToString().Replace("-", "");

                RunOnUiThread(async () =>
                {
                    try
                    {
                        TabbedMainActivity.GetInstance()?.SetOnWakeLock();
                        IsStreamingStarted = true;

                        if (IsOwner)
                        {
                            StartBroadcast();
                        }
                        else
                        {
                            StopBroadcast();
                            InitStreamerInfo();
                        }

                        if (IsOwner)
                        {
                            await AgoraAcquire(AppSettings.AppIdAgoraLive, ListUtils.SettingsSiteList?.AgoraCustomerId, ListUtils.SettingsSiteList?.AgoraCustomerCertificate, channel, UidLive);
                        }
                        else
                        {
                            await Task.Delay(TimeSpan.FromSeconds(3));
                        }
                        LoadMessages();

                        //CreateLiveThumbnail(); 
                    }
                    catch (Exception e)
                    {
                        Methods.DisplayReportResultTrack(e);
                    }
                });
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public void OnUserOffline(int uid, int reason)
        {
            try
            {
                RunOnUiThread(() =>
                {
                    RemoveRemoteUser(uid);
                });
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public void OnUserJoined(int uid, int elapsed)
        {
           
        }

        public void OnLastmileQuality(int quality)
        {
             
        }

        public void OnLastmileProbeResult(IRtcEngineEventHandler.LastmileProbeResult result)
        {
            
        }

        public void OnLocalVideoStats(Constants.VideoSourceType source, IRtcEngineEventHandler.LocalVideoStats stats)
        {
            try
            {
                if (!StatsManager().IsEnabled()) return;

                LocalStatsData data = (LocalStatsData)StatsManager().GetStatsData(0);
                if (data == null) return;

                data.SetWidth(MVideoDimension.Width);
                data.SetHeight(MVideoDimension.Height);
                data.SetFramerate(stats.SentFrameRate);
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public void OnRtcStats(IRtcEngineEventHandler.RtcStats stats)
        {
            try
            {
                if (!StatsManager().IsEnabled()) return;

                LocalStatsData data = (LocalStatsData)StatsManager().GetStatsData(0);
                if (data == null) return;

                data.SetLastMileDelay(stats.LastmileDelay);
                data.SetVideoSendBitrate(stats.TxVideoKBitRate);
                data.SetVideoRecvBitrate(stats.RxVideoKBitRate);
                data.SetAudioSendBitrate(stats.TxAudioKBitRate);
                data.SetAudioRecvBitrate(stats.RxAudioKBitRate);
                data.SetCpuApp(stats.CpuAppUsage);
                data.SetCpuTotal(stats.CpuAppUsage);
                data.SetSendLoss(stats.TxPacketLossRate);
                data.SetRecvLoss(stats.RxPacketLossRate);
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public void OnNetworkQuality(int uid, int txQuality, int rxQuality)
        {
            try
            {
                if (!StatsManager().IsEnabled()) return;

                StatsData data = StatsManager().GetStatsData(uid);
                if (data == null)
                {
                    return;
                }
                else
                {
                    data.SetSendQuality(StatsManager().QualityToString(txQuality));
                    data.SetRecvQuality(StatsManager().QualityToString(rxQuality));
                }
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public void OnRemoteVideoStats(IRtcEngineEventHandler.RemoteVideoStats stats)
        {
            try
            {
                if (!StatsManager().IsEnabled()) return;

                RemoteStatsData data = (RemoteStatsData)StatsManager().GetStatsData(stats.Uid);
                if (data == null) return;

                data.SetWidth(stats.Width);
                data.SetHeight(stats.Height);
                data.SetFramerate(stats.RendererOutputFrameRate);
#pragma warning disable 618
                data.SetVideoDelay(stats.Delay);
#pragma warning restore 618
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public void OnRemoteAudioStats(IRtcEngineEventHandler.RemoteAudioStats stats)
        {
            try
            {
                if (!StatsManager().IsEnabled()) return;

                RemoteStatsData data = (RemoteStatsData)StatsManager().GetStatsData(stats.Uid);
                if (data == null) return;

                data.SetAudioNetDelay(stats.NetworkTransportDelay);
                data.SetAudioNetJitter(stats.JitterBufferDelay);
                data.SetAudioLoss(stats.AudioLossRate);
                data.SetAudioQuality(StatsManager().QualityToString(stats.Quality));
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public void OnError(int err)
        {
            RunOnUiThread(() =>
            {
                try
                {
                    Console.WriteLine("Error code " + err);

                    var dialog = new MaterialAlertDialogBuilder(this);
                    dialog.SetTitle(GetText(Resource.String.Lbl_ErrorLive_Code) + " " + err);
                    dialog.SetMessage(GetText(Resource.String.Lbl_ErrorCall_Message));
                    dialog.SetPositiveButton(GetText(Resource.String.Lbl_Ok), (materialDialog, action) =>
                    {
                        try
                        {
                            Finish();
                        }
                        catch (Exception e)
                        {
                            Methods.DisplayReportResultTrack(e);
                        }
                    });
                    dialog.SetNeutralButton(GetText(Resource.String.Lbl_ContactUs), (materialDialog, action) =>
                    {
                        try
                        {
                            new IntentController(this).OpenBrowserFromApp(InitializeWoWonder.WebsiteUrl + "/contact-us");
                            Finish();
                        }
                        catch (Exception e)
                        {
                            Methods.DisplayReportResultTrack(e);
                        }
                    });
                    dialog.Show();
                }
                catch (Exception e)
                {
                    Finish();
                    Methods.DisplayReportResultTrack(e);
                }
            });
        }

        private void RenderRemoteUser(int uid)
        {
            try
            {
                SurfaceView = PrepareRtcVideo(uid, false);
                MVideoLayout.AddUserVideotextureView(uid, SurfaceView, false);
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void RemoveRemoteUser(int uid)
        {
            try
            {
                RemoveRtcVideo(uid, false);
                MVideoLayout.RemoveUserVideo(uid, false);
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        #endregion
        
        #region Filters

        private string RenderType = "";

        private LiveFiltersAdapter LiveFiltersAdapter;
        private LinearLayout MaskLayout;
        private ImageView MaskClose;
        private RecyclerView MaskRecycler;
        private AppCompatSeekBar MaskSeekBar;

        private void InitFilters()
        {
            try
            {
                MaskLayout = FindViewById<LinearLayout>(Resource.Id.MaskLayout);
                MaskClose = FindViewById<ImageView>(Resource.Id.Mask_close);
                MaskRecycler = FindViewById<RecyclerView>(Resource.Id.MaskRecycler);
                MaskSeekBar = FindViewById<AppCompatSeekBar>(Resource.Id.MaskSeekBar);

                LiveFiltersAdapter = new LiveFiltersAdapter(this)
                {
                    FilterList = new ObservableCollection<OptionItem>()
                };
                LiveFiltersAdapter.ItemClick += LiveFiltersAdapterOnItemClick;
                LayoutManager = new LinearLayoutManager(this, LinearLayoutManager.Horizontal, false);
                MaskRecycler.SetLayoutManager(LayoutManager);
                MaskRecycler.HasFixedSize = true;
                MaskRecycler.SetItemViewCacheSize(50);
                MaskRecycler.GetLayoutManager().ItemPrefetchEnabled = true;
                var sizeProvider = new FixedPreloadSizeProvider(10, 10);
                var preLoader = new RecyclerViewPreloader<CommentObjectExtra>(this, LiveFiltersAdapter, sizeProvider, 10);
                MaskRecycler.AddOnScrollListener(preLoader);
                MaskRecycler.SetAdapter(LiveFiltersAdapter);

                MaskLayout.Visibility = ViewStates.Gone;
                MaskSeekBar.Visibility = ViewStates.Gone;
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void MaskSeekBarOnProgressChanged(object sender, SeekBar.ProgressChangedEventArgs e)
        {
            try
            {
                if (RenderType == "AgoraRender")
                {
                    AgoraRender.SetCurrentProgress(e.Progress);
                }
                else if (RenderType == "BackgroundRender")
                {
                    BackgroundRender.SetupBackgroundBlur(e.Progress);
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private async void LiveFiltersAdapterOnItemClick(object sender, LiveFiltersAdapterClickEventArgs e)
        {
            try
            {
                var item = LiveFiltersAdapter.GetItem(e.Position);
                if (item != null)
                {
                    LiveFiltersAdapter.Click_item(item);

                    if (RenderType == "AgoraRender")
                    {
                        AgoraRender.SetSelectedId(item.Id);

                        if (item.Id == -1)
                        {
                            AgoraRender.DisableExtension();
                            MaskSeekBar.Visibility = ViewStates.Gone;
                        }
                        else
                        {
                            AgoraRender.EnableExtension();
                            MaskSeekBar.Visibility = ViewStates.Visible;
                        }

                        if (Build.VERSION.SdkInt >= BuildVersionCodes.N)
                            MaskSeekBar.SetProgress(AgoraRender.GetCurrentProgress(), true);
                        else
                            // For API < 24 
                            MaskSeekBar.Progress = AgoraRender.GetCurrentProgress();
                    }
                    else if (RenderType == "BackgroundRender")
                    { 
                        if (item.Id == BackgroundRender.IdOriginal)
                        {
                            MaskSeekBar.Visibility = ViewStates.Gone;
                            BackgroundRender.RemoveBackground();
                        }
                        else if (item.Id == BackgroundRender.IdBackgroundBlur)
                        {
                            MaskSeekBar.Visibility = ViewStates.Visible;
                            BackgroundRender.SetupBackgroundBlur(MaskSeekBar.Progress);
                        }
                        else if (item.Id == BackgroundRender.IdBackgroundSolid)
                        {
                            MaskSeekBar.Visibility = ViewStates.Gone;
                            BackgroundRender.SetSolidBackground();
                        }
                        else if (item.Id == BackgroundRender.IdCustomize)
                        {
                            MaskSeekBar.Visibility = ViewStates.Gone;
                            //open gallary image
                        }
                        else
                        {
                            if (!string.IsNullOrEmpty(item.ImgUrl))
                            {
                                var path = await BackgroundRender.GetBackgroundPath(item.ImgUrl);

                                MaskSeekBar.Visibility = ViewStates.Gone;
                                BackgroundRender.SetVirtualImgBg(path);
                            } 
                        } 
                    }
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }
         
        private void MaskCloseOnClick(object sender, EventArgs e)
        {
            try
            {
                RenderType = "";

                MaskLayout.Visibility = ViewStates.Gone;
                ContentView.Visibility = ViewStates.Visible;
                MaskSeekBar.Visibility = ViewStates.Gone;
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void ShowMask(string type)
        {
            try
            {
                RenderType = type;
                if (type == "FuRender")
                {
                    MaskLayout.Visibility = ViewStates.Visible;
                    ContentView.Visibility = ViewStates.Gone;
                    MaskSeekBar.Visibility = ViewStates.Gone;
                    MaskSeekBar.Max = 10;

                    //LiveFiltersAdapter.FilterList = new ObservableCollection<OptionItem>(FuRender.GeneratorOptionItems());
                    //LiveFiltersAdapter.NotifyDataSetChanged();
                }
                else if (type == "AgoraRender")
                {
                    MaskLayout.Visibility = ViewStates.Visible;
                    ContentView.Visibility = ViewStates.Gone;
                    MaskSeekBar.Visibility = ViewStates.Gone;
                    MaskSeekBar.Max = 10;

                    LiveFiltersAdapter.FilterList = new ObservableCollection<OptionItem>(AgoraRender.GeneratorOptionItems());
                    LiveFiltersAdapter.NotifyDataSetChanged();
                }
                else if (type == "BackgroundRender")
                {
                    MaskLayout.Visibility = ViewStates.Visible;
                    ContentView.Visibility = ViewStates.Gone;
                    MaskSeekBar.Visibility = ViewStates.Gone;

                    MaskSeekBar.Max = 100;

                    LiveFiltersAdapter.FilterList = new ObservableCollection<OptionItem>(BackgroundRender.GeneratorOptionItems());
                    LiveFiltersAdapter.NotifyDataSetChanged();
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        #endregion
        
        #region Api

        private void CreateLiveStream()
        {
            if (!Methods.CheckConnectivity())
                ToastUtils.ShowToast(this, GetString(Resource.String.Lbl_CheckYourInternetConnection), ToastLength.Short);
            else
                PollyController.RunRetryPolicyFunction(new List<Func<Task>> { CreateLive });
        }

        private async Task CreateLive()
        {
            var (apiStatus, respond) = await RequestsAsync.Posts.CreateLiveAsync(Config().GetChannelName());
            if (apiStatus == 200)
            {
                if (respond is AddPostObject result)
                {
                    PostObject = result.PostData;
                    PostId = result.PostData.PostId;

                    JoinChannel();
                }
            }
            else
                Methods.DisplayReportResult(this, respond);
        }

        private static string Base64Encode(string plainText)
        {
            var plainTextBytes = Encoding.UTF8.GetBytes(plainText);
            return Convert.ToBase64String(plainTextBytes);
        }

        #region Agora Record

        private async Task AgoraAcquire(string appid, string customerId, string customerCertificate, string channel, string uid)
        {
            try
            {
                var url = "https://api.agora.io/v1/apps/" + appid + "/cloud_recording/acquire";

                using var httpClient = new HttpClient();
                httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Authorization", "Basic " + Base64Encode(customerId + ":" + customerCertificate));
                //httpClient.DefaultRequestHeaders.TryAddWithoutValidation("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36 Edge/18.18363");
                httpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json; charset=utf-8");
                httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
                httpClient.Timeout = TimeSpan.FromMinutes(3.0);
                try { httpClient.BaseAddress = new System.Uri(url); } catch (Exception) { }

                using var request = new HttpRequestMessage
                {
                    Method = HttpMethod.Post,
                    RequestUri = new System.Uri(url),
                    Content = new StringContent("{\n  \"cname\": \"" + channel + "\",\n  \"uid\": \"" + uid + "\",\n  \"clientRequest\":{\n  }\n}", Encoding.UTF8, "application/json"),
                };

                request.Content.Headers.ContentType = new MediaTypeHeaderValue("application/json");
                request.Headers.Add("Connection", new[] { "Keep-Alive" });

                var response = await httpClient.SendAsync(request);

                string json = await response.Content.ReadAsStringAsync();
                var data = JsonConvert.DeserializeObject<AgoraRecordObject>(json);
                if (data?.ResourceId != null)
                {
                    ResourceId = data.ResourceId;
                    await AgoraStart(appid, customerId, customerCertificate, ResourceId, channel, uid);
                }
            }
            catch (NotSupportedException e) // When content type is not valid => The content type is not supported
            {
                Methods.DisplayReportResultTrack(e);
            }
            catch (JsonException e) // Invalid JSON
            {
                Methods.DisplayReportResultTrack(e);
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private async Task AgoraStart(string appid, string customerId, string customerCertificate, string resourceId, string channel, string uid)
        {
            try
            {
                var url = "https://api.agora.io/v1/apps/" + appid + "/cloud_recording/resourceid/" + resourceId + "/mode/mix/start";

                using var httpClient = new HttpClient();
                httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Authorization", "Basic " + Base64Encode(customerId + ":" + customerCertificate));
                //httpClient.DefaultRequestHeaders.TryAddWithoutValidation("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36 Edge/18.18363");
                httpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json; charset=utf-8");
                httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
                httpClient.Timeout = TimeSpan.FromMinutes(3.0);
                try { httpClient.BaseAddress = new System.Uri(url); }
                catch (Exception)
                { // ignored
                }

                var storageVendor = "1";
                var region = GetRegion(ListUtils.SettingsSiteList?.Region2);
                var bucket = ListUtils.SettingsSiteList?.BucketName2;
                var accessKey = ListUtils.SettingsSiteList?.AmazoneS3Key2;
                var secretKey = ListUtils.SettingsSiteList?.AmazoneS3SKey2;

                var jsonBody = "{\n\t\"cname\":\"" + channel + "\",\n\t\"uid\":\"" + uid + "\"," +
                               "\n\t\"clientRequest\":{\n\t\t\"recordingConfig\":{\n\t\t\t\"channelType\":1,\n\t\t\t\"streamTypes\":2,\n\t\t\t\"audioProfile\":1,\n\t\t\t\"videoStreamType\":1,\n\t\t\t\"maxIdleTime\":120,\n\t\t\t\"transcodingConfig\":{\n\t\t\t\t\"width\":480,\n\t\t\t\t\"height\":480,\n\t\t\t\t\"fps\":24,\n\t\t\t\t\"bitrate\":800,\n\t\t\t\t\"maxResolutionUid\":\"1\"," +
                               "\n\t\t\t\t\"mixedVideoLayout\":1\n\t\t\t\t}\n\t\t\t},\n\t\t\"storageConfig\":" +
                               "{\n\t\t\t\"vendor\":" + storageVendor + ",\n\t\t\t\"region\":" + region + "," +
                               "\n\t\t\t\"bucket\":\"" + bucket + "\",\n\t\t\t\"accessKey\":\"" + accessKey + "\"," +
                               "\n\t\t\t\"secretKey\":\"" + secretKey + "\"\n\t\t}\t\n\t}\n} \n";

                using var request = new HttpRequestMessage
                {
                    Method = HttpMethod.Post,
                    RequestUri = new System.Uri(url),
                    Content = new StringContent(jsonBody, Encoding.UTF8, "application/json"),
                };

                request.Content.Headers.ContentType = new MediaTypeHeaderValue("application/json");
                request.Headers.Add("Connection", new[] { "Keep-Alive" });

                var response = await httpClient.SendAsync(request);

                string json = await response.Content.ReadAsStringAsync();
                var data = JsonConvert.DeserializeObject<AgoraRecordObject>(json);
                if (data?.Sid != null)
                {
                    SId = data.Sid;
                    await LoadDataComment(resourceId, SId);
                }
            }
            catch (NotSupportedException e) // When content type is not valid => The content type is not supported
            {
                Methods.DisplayReportResultTrack(e);
            }
            catch (JsonException e) // Invalid JSON
            {
                Methods.DisplayReportResultTrack(e);
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private async Task AgoraStop(string appid, string customerId, string customerCertificate, string resourceId, string sid, string channel, string uid)
        {
            try
            {
                var url = "https://api.agora.io/v1/apps/" + appid + "/cloud_recording/resourceid/" + resourceId + "/sid/" + sid + "/mode/mix/stop";

                using var httpClient = new HttpClient();
                httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Authorization", "Basic " + Base64Encode(customerId + ":" + customerCertificate));
                // httpClient.DefaultRequestHeaders.TryAddWithoutValidation("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36 Edge/18.18363");
                httpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json; charset=utf-8");
                httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
                httpClient.Timeout = TimeSpan.FromMinutes(3.0);
                try { httpClient.BaseAddress = new System.Uri(url); } catch (Exception) { }

                using var request = new HttpRequestMessage
                {
                    Method = HttpMethod.Post,
                    RequestUri = new System.Uri(url),
                    Content = new StringContent("{\n  \"cname\": \"" + channel + "\",\n  \"uid\": \"" + uid + "\",\n  \"clientRequest\":{\n  }\n}", Encoding.UTF8, "application/json"),
                };

                request.Content.Headers.ContentType = new MediaTypeHeaderValue("application/json");
                request.Headers.Add("Connection", new[] { "Keep-Alive" });

                var response = await httpClient.SendAsync(request);

                string json = await response.Content.ReadAsStringAsync();
                var data = JsonConvert.DeserializeObject<AgoraRecordObject>(json);
                if (data?.ServerResponse?.FileList != null)
                {
                    FileListLive = data.ServerResponse?.FileList;
                    await LoadDataComment("", "", FileListLive);
                }
            }
            catch (NotSupportedException e) // When content type is not valid => The content type is not supported
            {
                Methods.DisplayReportResultTrack(e);
            }
            catch (JsonException e) // Invalid JSON
            {
                Methods.DisplayReportResultTrack(e);
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        #endregion

        private void DeleteLiveStream()
        {
            if (!Methods.CheckConnectivity())
                ToastUtils.ShowToast(this, GetString(Resource.String.Lbl_CheckYourInternetConnection), ToastLength.Short);
            else
                PollyController.RunRetryPolicyFunction(new List<Func<Task>> { async () => await RequestsAsync.Posts.DeleteLiveAsync(PostId) });
        }

        #region CreateLiveThumbnail


        //private void CreateLiveThumbnail()
        //{
        //    try
        //    {
        //        if (!Methods.CheckConnectivity())
        //            ToastUtils.ShowToast(this, GetString(Resource.String.Lbl_CheckYourInternetConnection), ToastLength.Short);
        //        else
        //        {
        //            GetSurfaceBitmap(SurfaceView);
        //        }
        //    }
        //    catch (Exception e)
        //    {
        //        Methods.DisplayReportResultTrack(e);  
        //    }
        //}

        //private Bitmap SurfaceBitmap;
        //private void GetSurfaceBitmap(SurfaceView surfaceView)
        //{
        //    try
        //    {
        //        if (surfaceView == null)
        //            return;

        //        if (surfaceView.MeasuredHeight <= 0)
        //            surfaceView.Measure(ViewGroup.LayoutParams.WrapContent, ViewGroup.LayoutParams.WrapContent);

        //        SurfaceBitmap = Bitmap.CreateBitmap(surfaceView.Width, surfaceView.Height, Bitmap.Config.Argb8888);
        //        if (SurfaceBitmap != null)
        //        {
        //            HandlerThread handlerThread = new HandlerThread(PostId + "_thumbnail");
        //            handlerThread.Start();

        //            if (Build.VERSION.SdkInt >= BuildVersionCodes.N)
        //            {
        //                PixelCopy.Request(surfaceView, SurfaceBitmap, this, new Handler(handlerThread.Looper));
        //            }
        //            else
        //            {
        //                Console.WriteLine("Saving an image of a SurfaceView is only supported for API 24 and above");
        //            }
        //        } 
        //    }
        //    catch (Exception e)
        //    {
        //        Methods.DisplayReportResultTrack(e);
        //    }
        //}

        //public void OnPixelCopyFinished(int copyResult)
        //{
        //    try
        //    {
        //        if (copyResult == (int)PixelCopyResult.Success)
        //        {
        //            var pathImage = Methods.MultiMedia.Export_Bitmap_As_Image(SurfaceBitmap, PostId + "_thumbnail", Methods.Path.FolderDcimImage);
        //            if (!string.IsNullOrEmpty(pathImage))
        //            {
        //                PollyController.RunRetryPolicyFunction(new List<Func<Task>> { () => RequestsAsync.Posts.CreateLiveThumbnail(PostId, pathImage) });
        //            }
        //        }
        //    }
        //    catch (Exception e)
        //    {
        //        Methods.DisplayReportResultTrack(e);
        //    }
        //}

        #endregion

        private void LoadMessages()
        {
            if (!Methods.CheckConnectivity())
                ToastUtils.ShowToast(this, GetString(Resource.String.Lbl_CheckYourInternetConnection), ToastLength.Short);
            else
                PollyController.RunRetryPolicyFunction(new List<Func<Task>> { async () => await LoadDataComment() });
        }

        private async Task LoadDataComment(string resourceId = "", string sid = "", string fileList = "")
        {
            if (string.IsNullOrEmpty(PostId))
                return;

            if (Methods.CheckConnectivity())
            {
                var offset = MAdapter.CommentList.LastOrDefault()?.Id ?? "0";
                var (apiStatus, respond) = await RequestsAsync.Posts.CheckCommentsLiveAsync(PostId, IsOwner ? "live" : "story", "10", offset, resourceId, sid, fileList);
                if (apiStatus != 200 || respond is not CheckCommentsLiveObject result || result.Comments == null)
                {
                    if (respond is ErrorObject error)
                    {
                        if (error.Error.ErrorText == "post not found")
                        {
                            RunOnUiThread(() => { FinishStreaming(IsOwner); });
                        }
                    }
                    else
                        Methods.DisplayReportResult(this, respond);
                }
                else
                {
                    var respondList = result.Comments?.Count;
                    if (respondList > 0)
                    {
                        foreach (var item in result.Comments)
                        {
                            CommentObjectExtra check = MAdapter.CommentList.FirstOrDefault(a => a.Id == item.Id);
                            if (check == null)
                            {
                                var db = ClassMapper.Mapper?.Map<CommentObjectExtra>(item);
                                if (db != null) MAdapter.CommentList.Add(db);
                            }
                            else
                            {
                                check = ClassMapper.Mapper?.Map<CommentObjectExtra>(item);
                                check.Replies = item.Replies;
                                check.RepliesCount = item.RepliesCount;
                            }
                        }

                        RunOnUiThread(() => { MAdapter.NotifyDataSetChanged(); });
                    }

                    RunOnUiThread(() =>
                    {
                        try
                        {
                            if (result.Count != null)
                                MViewersText.Text = Methods.FunString.FormatPriceValue(result.Count.Value) + " " + GetText(Resource.String.Lbl_Views);
                            else
                                MViewersText.Text = "0 " + GetText(Resource.String.Lbl_Views);

                            if (TimerComments != null && !string.IsNullOrEmpty(result.StillLive) && result.StillLive == "offline")
                                BackPressed();
                        }
                        catch (Exception exception)
                        {
                            Methods.DisplayReportResultTrack(exception);
                        }
                    });
                }

                RunOnUiThread(() =>
                {
                    try
                    {
                        MRecycler.Visibility = ViewStates.Visible;
                        var index = MAdapter.CommentList.IndexOf(MAdapter.CommentList.LastOrDefault());
                        if (index > -1) MRecycler.ScrollToPosition(index);

                        SetTimerComment();
                    }
                    catch (Exception exception)
                    {
                        Methods.DisplayReportResultTrack(exception);
                    }
                });
            }
        }

        #endregion

        #region Timer Time LIve

        private void SetTimer(long elapsed)
        {
            try
            {
                if (IsOwner)
                {
                    StartTime = elapsed;
                    CustomHandler ??= new Handler(Looper.MainLooper);
                    UpdateTimerThread = new MyRunnable(this);
                    CustomHandler.PostDelayed(UpdateTimerThread, 0);
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void StopTimer()
        {
            try
            {
                if (IsOwner)
                {
                    TimeSwapBuff += TimeInMilliseconds;
                    CustomHandler.RemoveCallbacks(UpdateTimerThread);
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private class MyRunnable : Object, IRunnable
        {
            private readonly LiveStreamingActivity Activity;
            public MyRunnable(LiveStreamingActivity activity)
            {
                Activity = activity;
            }

            public void Run()
            {
                try
                {
                    Activity.TimeInMilliseconds = SystemClock.ElapsedRealtime() - Activity.StartTime;
                    Activity.UpdatedTime = Activity.TimeSwapBuff + Activity.TimeInMilliseconds;
                    int secs = (int)(Activity.UpdatedTime / 1000);
                    int min = secs / 60;
                    secs %= 60;

                    TimeSpan tsTemp = new TimeSpan(0, min, secs);
                    Activity.MTimeText.Text = tsTemp.ToString();

                    Activity.CustomHandler.PostDelayed(this, 0);
                }
                catch (Exception exception)
                {
                    Methods.DisplayReportResultTrack(exception);
                }
            }
        }

        #endregion

        #region Timer Load Comment

        private void SetTimerComment()
        {
            try
            {
                if (TimerComments ==
                    //Run timer
                    null)
                {
                    TimerComments = new Timer { Interval = 3000 };
                    TimerComments.Elapsed += TimerCommentsOnElapsed;
                    TimerComments.Enabled = true;
                    TimerComments.Start();
                }
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void TimerCommentsOnElapsed(object sender, ElapsedEventArgs e)
        {
            try
            {
                LoadMessages();
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void StartTimerComment()
        {
            try
            {
                if (TimerComments != null)
                {
                    TimerComments.Enabled = true;
                    TimerComments.Start();
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void StopTimerComment()
        {
            try
            {
                if (TimerComments != null)
                {
                    TimerComments.Enabled = false;
                    TimerComments.Stop();
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        private void DestroyTimerComment()
        {
            try
            {
                if (TimerComments != null)
                {
                    TimerComments.Enabled = false;
                    TimerComments.Stop();
                    TimerComments.Dispose();
                    TimerComments = null!;
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        #endregion

        #region MaterialDialog

        public void OnSelection(IDialogInterface dialog, int position, string itemString)
        {
            try
            {
                if (itemString == GetText(Resource.String.Lbl_ViewProfile))
                {
                    if (IsOwner)
                        WoWonderTools.OpenProfile(this, PostObject.Publisher.UserId, PostObject.Publisher);
                    else
                        WoWonderTools.OpenProfile(this, LiveStreamViewerObject.Publisher.UserId, LiveStreamViewerObject.Publisher);
                }
                else if (itemString == GetText(Resource.String.Lbl_Copy))
                {
                    Methods.CopyToClipboard(this, IsOwner ? PostObject.Url : LiveStreamViewerObject.Url);
                }
                else if (itemString == GetText(Resource.String.Lbl_Report))
                {
                    var arrayAdapter = new List<string>();
                    var dialogList = new MaterialAlertDialogBuilder(this);
                    dialogList.SetTitle(GetText(Resource.String.Lbl_ReportLive_Title));
                    //dialogList.SetMessage(GetText(Resource.String.Lbl_ReportLive_desc));

                    arrayAdapter.Add(GetText(Resource.String.Lbl_Nudity));
                    arrayAdapter.Add(GetText(Resource.String.Lbl_Violence));
                    arrayAdapter.Add(GetText(Resource.String.Lbl_Harassment));
                    arrayAdapter.Add(GetText(Resource.String.Lbl_Suicide));
                    arrayAdapter.Add(GetText(Resource.String.Lbl_FalseInformation));
                    arrayAdapter.Add(GetText(Resource.String.Lbl_Spam));
                    arrayAdapter.Add(GetText(Resource.String.Lbl_UnauthorizedSales));
                    arrayAdapter.Add(GetText(Resource.String.Lbl_HateSpeech));
                    arrayAdapter.Add(GetText(Resource.String.Lbl_Terrorism));
                    arrayAdapter.Add(GetText(Resource.String.Lbl_IntellectualProperty));
                    arrayAdapter.Add(GetText(Resource.String.Lbl_SomethingElse));
                    arrayAdapter.Add(GetText(Resource.String.Lbl_Other));

                    dialogList.SetItems(arrayAdapter.ToArray(), new MaterialDialogUtils(arrayAdapter, this));
                    dialogList.SetPositiveButton(GetText(Resource.String.Lbl_Report), (materialDialog, action) =>
                    {
                        try
                        {
                            ToastUtils.ShowToast(this, GetText(Resource.String.Lbl_YourReportPost), ToastLength.Short);
                            //Sent Api >>
                            PollyController.RunRetryPolicyFunction(new List<Func<Task>> { () => RequestsAsync.Posts.PostActionsAsync(IsOwner ? PostObject.Id : LiveStreamViewerObject.Id, "report") });
                        }
                        catch (Exception e)
                        {
                            Methods.DisplayReportResultTrack(e);
                        }
                    });
                    dialogList.SetNegativeButton(GetText(Resource.String.Lbl_Cancel), new MaterialDialogUtils());

                    dialogList.Show();
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        #endregion

        private string GetRegion(string region)
        {
            try
            {
                return region switch
                {
                    "us-east-1" => "0",
                    "us-east-2" => "1",
                    "us-west-1" => "2",
                    "us-west-2" => "3",
                    "eu-west-1" => "4",
                    "eu-west-2" => "5",
                    "eu-west-3" => "6",
                    "eu-central-1" => "7",
                    "ap-southeast-1" => "8",
                    "ap-southeast-2" => "9",
                    "ap-northeast-1" => "10",
                    "ap-northeast-2" => "11",
                    "sa-east-1" => "12",
                    "ca-central-1" => "13",
                    "ap-south-1" => "14",
                    "cn-north-1" => "15",
                    "us-gov-west-1" => "17",
                    _ => ""
                };
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
                return "";
            }
        }
    }
}