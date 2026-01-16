using Android.Views;
using Android.Widget;
using AndroidX.RecyclerView.Widget;
using Bumptech.Glide;
using Java.Util;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using Android.App;
using Android.Graphics;
using WoWonder.Helpers.CacheLoaders;
using WoWonder.Helpers.Utils;
using IList = System.Collections.IList;
using Object = Java.Lang.Object;
using WoWonder.Activities.Live.Filters;

namespace WoWonder.Activities.Live.Adapters
{
    public class LiveFiltersAdapter : RecyclerView.Adapter, ListPreloader.IPreloadModelProvider
    {
        public event EventHandler<LiveFiltersAdapterClickEventArgs> ItemClick;
        public event EventHandler<LiveFiltersAdapterClickEventArgs> ItemLongClick;

        private readonly Activity ActivityContext;
        public ObservableCollection<OptionItem> FilterList = new ObservableCollection<OptionItem>();

        public LiveFiltersAdapter(Activity context)
        {
            try
            {
                HasStableIds = true;
                ActivityContext = context;
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public override int ItemCount => FilterList?.Count ?? 0;

        // Create new views (invoked by the layout manager)
        public override RecyclerView.ViewHolder OnCreateViewHolder(ViewGroup parent, int viewType)
        {
            try
            {
                //Setup your layout here >> Style_LiveFilter
                var itemView = LayoutInflater.From(parent.Context)?.Inflate(Resource.Layout.Style_LiveFilter, parent, false);
                var vh = new LiveFiltersAdapterViewHolder(itemView, Click, LongClick);
                return vh;
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
                return null!;
            }
        }

        // Replace the contents of a view (invoked by the layout manager)
        public override void OnBindViewHolder(RecyclerView.ViewHolder viewHolder, int position)
        {
            try
            {
                if (viewHolder is LiveFiltersAdapterViewHolder holder)
                {
                    var item = FilterList[position];
                    if (item != null)
                    {
                        if (!string.IsNullOrEmpty(item.ImgUrl))
                        {
                            GlideImageLoader.LoadImage(ActivityContext, item.ImgUrl, holder.Image, ImageStyle.CircleCrop, ImagePlaceholders.Drawable);
                        }
                        else
                        {
                            holder.Image.SetImageResource(item.ImgRes);
                        }
                      
                        holder.TxtName.Text = ActivityContext.GetText(item.TitleRes);

                        if (item.Selected)
                        {
                            holder.TxtName.SetTextColor(Color.ParseColor(AppSettings.MainColor));
                        }
                        else
                        {
                            holder.TxtName.SetTextColor(Color.White);
                        }
                    }
                }
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        public override void OnViewRecycled(Object holder)
        {
            try
            {
                if (ActivityContext?.IsDestroyed != false)
                    return;

                switch (holder)
                {
                    case LiveFiltersAdapterViewHolder viewHolder:
                        Glide.With(ActivityContext?.BaseContext).Clear(viewHolder.Image);
                        break;
                }
                base.OnViewRecycled(holder);
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public OptionItem GetItem(int position)
        {
            return FilterList[position];
        }

        public override long GetItemId(int position)
        {
            try
            {
                return position;
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
                return 0;
            }
        }

        public override int GetItemViewType(int position)
        {
            try
            {
                return position;
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
                return 0;
            }
        }

        public void Click_item(OptionItem item)
        {
            try
            {
                var check = FilterList.Where(a => a.Selected).ToList();
                if (check.Count > 0)
                    foreach (var all in check)
                        all.Selected = false;

                var click = FilterList.FirstOrDefault(a => a.Id == item.Id);
                if (click != null) click.Selected = true;

                NotifyDataSetChanged();
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        public void UnClick_item()
        {
            try
            {
                var check = FilterList.Where(a => a.Selected).ToList();
                if (check.Count > 0)
                    foreach (var all in check)
                        all.Selected = false;
                 
                NotifyDataSetChanged();
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }

        private void Click(LiveFiltersAdapterClickEventArgs args)
        {
            ItemClick?.Invoke(this, args);
        }

        private void LongClick(LiveFiltersAdapterClickEventArgs args)
        {
            ItemLongClick?.Invoke(this, args);
        }

        public IList GetPreloadItems(int p0)
        {
            try
            {
                var d = new List<string>();
                var item = FilterList[p0];
                switch (item)
                {
                    case null:
                        return Collections.SingletonList(p0);
                }

                //switch (string.IsNullOrEmpty(item.TitleRes))
                //{
                //    case false:
                //        d.Add(item.Publisher.Avatar);
                //        break;
                //}
                return d;
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
                return Collections.SingletonList(p0);
            }
        }


        public RequestBuilder GetPreloadRequestBuilder(Object p0)
        {
            return GlideImageLoader.GetPreLoadRequestBuilder(ActivityContext, p0.ToString(), ImageStyle.RoundedCrop);
        }
    }

    public class LiveFiltersAdapterViewHolder : RecyclerView.ViewHolder
    {
        public LiveFiltersAdapterViewHolder(View itemView, Action<LiveFiltersAdapterClickEventArgs> clickListener, Action<LiveFiltersAdapterClickEventArgs> longClickListener) : base(itemView)
        {
            try
            {
                MainView = itemView;
                Image = itemView.FindViewById<ImageView>(Resource.Id.Icon);
                TxtName = itemView.FindViewById<TextView>(Resource.Id.Text); 

                //Event
                itemView.Click += (sender, e) => clickListener(new LiveFiltersAdapterClickEventArgs { View = itemView, Position = BindingAdapterPosition });
                itemView.LongClick += (sender, e) => longClickListener(new LiveFiltersAdapterClickEventArgs { View = itemView, Position = BindingAdapterPosition });
            }
            catch (Exception exception)
            {
                Methods.DisplayReportResultTrack(exception);
            }
        }

        #region Variables Basic

        public View MainView { get; }
        public ImageView Image { get; private set; }
        public TextView TxtName { get; private set; } 

        #endregion Variables Basic

    }

    public class LiveFiltersAdapterClickEventArgs : EventArgs
    {
        public View View { get; set; }
        public int Position { get; set; }
    }
}