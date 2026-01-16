using Android.Content;
using Android.OS;
using Android.Views;
using Android.Widget;
using AndroidX.RecyclerView.Widget;
using Google.Android.Material.BottomSheet;
using System;
using System.Collections.ObjectModel; 
using WoWonder.Activities.Tabbes;
using WoWonder.Adapters;
using WoWonder.Helpers.Model;
using WoWonder.Helpers.Utils;
using Exception = System.Exception;

namespace WoWonder.Activities.Story
{
    public class OptionsAddStoryBottomSheet : BottomSheetDialogFragment
    {
        #region Variables Basic

        private TabbedMainActivity GlobalContext;

        private ImageView IconClose;
        private TextView TitleText;

        private RecyclerView MRecycler;
        private LinearLayoutManager LayoutManager;
        private ItemOptionAdapter MAdapter;

        #endregion

        #region General

        public override void OnCreate(Bundle savedInstanceState)
        {
            try
            {
                base.OnCreate(savedInstanceState);
                // Create your fragment here
                GlobalContext = TabbedMainActivity.GetInstance();
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public override View OnCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            try
            {
                Context contextThemeWrapper = WoWonderTools.IsTabDark() ? new ContextThemeWrapper(Activity, Resource.Style.MyTheme_Dark) : new ContextThemeWrapper(Activity, Resource.Style.MyTheme);
                // clone the inflater using the ContextThemeWrapper
                LayoutInflater localInflater = inflater.CloneInContext(contextThemeWrapper);

                View view = localInflater?.Inflate(Resource.Layout.BottomSheetDefaultLayout, container, false);
                return view;
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
                return null!;
            }
        }

        public override void OnViewCreated(View view, Bundle savedInstanceState)
        {
            try
            {
                base.OnViewCreated(view, savedInstanceState);
                InitComponent(view);
                SetRecyclerViewAdapters(view);

                LoadData();
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        public override void OnLowMemory()
        {
            try
            {
                GC.Collect(GC.MaxGeneration);
                base.OnLowMemory();
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        #endregion

        #region Functions

        private void InitComponent(View view)
        {
            try
            {
                IconClose = view.FindViewById<ImageView>(Resource.Id.iconClose);
                IconClose.Click += IconCloseOnClick;

                TitleText = view.FindViewById<TextView>(Resource.Id.titleText);
                TitleText.Text = GetText(Resource.String.Lbl_Addnewstory);
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void SetRecyclerViewAdapters(View view)
        {
            try
            {
                MRecycler = (RecyclerView)view.FindViewById(Resource.Id.recyler);

                MAdapter = new ItemOptionAdapter(Activity)
                {
                    ItemOptionList = new ObservableCollection<Classes.ItemOptionObject>()
                };
                MAdapter.ItemClick += MAdapterOnItemClick;
                LayoutManager = new LinearLayoutManager(Context);
                MRecycler.SetLayoutManager(LayoutManager);
                MRecycler.SetAdapter(MAdapter);
                MRecycler.HasFixedSize = true;
                MRecycler.SetItemViewCacheSize(50);
                MRecycler.GetLayoutManager().ItemPrefetchEnabled = true;
                MRecycler.GetRecycledViewPool().Clear();
                MRecycler.SetAdapter(MAdapter);
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        #endregion

        #region Event

        private void IconCloseOnClick(object sender, EventArgs e)
        {
            Dismiss();
        }

        private void MAdapterOnItemClick(object sender, ItemOptionAdapterClickEventArgs e)
        {
            try
            {
                var position = e.Position;
                if (position > -1)
                {
                    var item = MAdapter.GetItem(position);
                    if (item?.Id == "1") //text
                    {
                        GlobalContext.OpenEditColor();
                    }
                    else if (item?.Id == "2") //image
                    {
                        GlobalContext.OnImage_Button_Click();
                    }
                    else if (item?.Id == "3") //Camera
                    {
                        GlobalContext.OnCamera_Button_Click();
                    }
                    Dismiss();
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        #endregion

        private void LoadData()
        {
            try
            {
                MAdapter.ItemOptionList.Add(new Classes.ItemOptionObject()
                {
                    Id = "1",
                    Text = GetText(Resource.String.text),
                    Icon = Resource.Drawable.icon_color_vector,
                });

                MAdapter.ItemOptionList.Add(new Classes.ItemOptionObject()
                {
                    Id = "2",
                    Text = GetText(Resource.String.Lbl_Gallery),
                    Icon = Resource.Drawable.icon_image_vector,
                });

                if (AppSettings.EnableDeepAr)
                    MAdapter.ItemOptionList.Add(new Classes.ItemOptionObject()
                    {
                        Id = "3",
                        Text = GetText(Resource.String.Lbl_Camera),
                        Icon = Resource.Drawable.icon_camera_vector,
                    });

                MAdapter.NotifyDataSetChanged();
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }
    }
}