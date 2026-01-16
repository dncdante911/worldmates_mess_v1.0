using IO.Agora.Rtc2.Video;
using IO.Agora.Rtc2;
using System;
using System.Collections.Generic;
using Android.Content;
using Android.Icu.Text;
using Java.Lang;
using Exception = System.Exception;

namespace WoWonder.Activities.Live.Filters
{
    public class AgoraRender
    {
        public static readonly int IdAgoraContrast = 1;
        public static readonly int IdAgoraLightening = 2;
        public static readonly int IdAgoraSmoothness = 3;
        public static readonly int IdAgoraRedness = 4;
        public static readonly int IdAgoraSharpness = 5;

        private readonly RtcEngine RtcEngine;
        private Context Context;
        private int SelectedFeatureId = -1;
        private readonly BeautyOptions BeautyOptions = new BeautyOptions();
        private readonly Dictionary<int, int> Map = new Dictionary<int, int>();
        public AgoraRender(Context context, RtcEngine rtcEngine)
        {
            try
            {
                Context = context;
                RtcEngine = rtcEngine;
                BeautyOptions.LighteningContrastLevel = 2;
                BeautyOptions.LighteningLevel = 0.8f;
                BeautyOptions.SmoothnessLevel = 0.7f;
                BeautyOptions.RednessLevel = 0.5f;
                BeautyOptions.SharpnessLevel = 0.3f;
                Map.Add(IdAgoraContrast, 60);
                Map.Add(IdAgoraLightening, 80);
                Map.Add(IdAgoraSmoothness, 70);
                Map.Add(IdAgoraRedness, 50);
                Map.Add(IdAgoraSharpness, 30);
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
            }
        }

        public void DisableExtension()
        {
            RtcEngine.SetBeautyEffectOptions(false, BeautyOptions);
        }

        public void EnableExtension()
        {
            RtcEngine.SetBeautyEffectOptions(true, BeautyOptions);
        }

        public void SetSelectedId(int featureItem)
        {
            this.SelectedFeatureId = featureItem;
        }

        public int GetCurrentProgress()
        {
            if (SelectedFeatureId == -1)
            {
                return 0;
            }
            return Map.GetValueOrDefault(SelectedFeatureId, 0);
        }

        private readonly int ContrastMax = 2;
        public void SetCurrentProgress(int progress)
        {
            Map.Add(SelectedFeatureId, progress);
            DecimalFormat fnum = new DecimalFormat("##0.0");
            if (SelectedFeatureId == IdAgoraContrast)
            {
                BeautyOptions.LighteningContrastLevel = progress == 100 ? 2 : 3 * progress / 100;
            }
            else if (SelectedFeatureId == IdAgoraLightening)
            {
                BeautyOptions.LighteningLevel = Float.ValueOf(fnum.Format(1.0f * progress / 100)).FloatValue();
            }
            else if (SelectedFeatureId == IdAgoraSmoothness)
            {
                BeautyOptions.SmoothnessLevel = Float.ValueOf(fnum.Format(1.0f * progress / 100)).FloatValue();
            }
            else if (SelectedFeatureId == IdAgoraRedness)
            {
                BeautyOptions.RednessLevel = Float.ValueOf(fnum.Format(1.0f * progress / 100)).FloatValue();
            }
            else if (SelectedFeatureId == IdAgoraSharpness)
            {
                BeautyOptions.SharpnessLevel = Float.ValueOf(fnum.Format(1.0f * progress / 100)).FloatValue();
            }
            RtcEngine.SetBeautyEffectOptions(true, BeautyOptions);
        }

        public List<OptionItem> GeneratorOptionItems()
        {
            List<OptionItem> optionItems = new List<OptionItem>
            {
                new OptionItem()
                {
                    Id = IdAgoraSmoothness,
                    ImgRes = Resource.Drawable.ic_mopi,
                    TitleRes = Resource.String.Lbl_Buffing,
                },
                new OptionItem()
                {
                    Id = IdAgoraLightening,
                    ImgRes = Resource.Drawable.ic_meibai,
                    TitleRes = Resource.String.Lbl_Whiten,
                },
                new OptionItem()
                {
                    Id = IdAgoraRedness,
                    ImgRes = Resource.Drawable.ic_hongrun,
                    TitleRes = Resource.String.Lbl_Ruddy,
                },
                new OptionItem()
                {
                    Id = IdAgoraContrast,
                    ImgRes = Resource.Drawable.ic_mopi,
                    TitleRes = Resource.String.Lbl_Contrast,
                },
                new OptionItem()
                {
                    Id = IdAgoraSharpness,
                    ImgRes = Resource.Drawable.ic_sharpen,
                    TitleRes = Resource.String.Lbl_Sharpen,
                },
            };

            return optionItems;
        }
    }
}
