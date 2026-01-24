using IO.Agora.Rtc2.Video;
using IO.Agora.Rtc2;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Android.Content;
using WoWonder.Helpers.Utils;

namespace WoWonder.Activities.Live.Filters
{
    public class BackgroundRender
    {
        public static readonly int IdOriginal = 1;
        public static readonly int IdBackgroundBlur = 2;
        // public static readonly int IdSplitGreenScreen = 3;
        public static readonly int IdBackgroundSolid = 3;
        public static readonly int IdSunset = 4;
        public static readonly int IdOffice = 5;
        public static readonly int IdCafe = 6;
        public static readonly int IdLandscape = 7;
        public static readonly int IdMeetingRoom = 8;
        public static readonly int IdCustomize = 9;
        public static readonly int GalleryRequestCode = 1000;

        private readonly RtcEngine RtcEngine;
        private Context Context;
        private VirtualBackgroundSource VirtualBackgroundSource = new VirtualBackgroundSource();
        private SegmentationProperty SegmentationProperty = new SegmentationProperty();
        public bool SplitGreenEnabled = false;

        public BackgroundRender(Context context, RtcEngine rtcEngine)
        {
            try
            {
                Context = context;
                RtcEngine = rtcEngine;

            }
            catch (Exception e)
            {
                Console.WriteLine(e);
            }
        }
        public bool IsFeatureAvailable()
        {
            return RtcEngine.IsFeatureAvailableOnDevice(Constants.FeatureVideoVirtualBackground);
        }

        public void RemoveBackground()
        {
            try
            {
                VirtualBackgroundSource = new VirtualBackgroundSource();
                SegmentationProperty = new SegmentationProperty();
                // Disable virtual background
                RtcEngine.EnableVirtualBackground(false, VirtualBackgroundSource, SegmentationProperty);
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
            }
        }

        public void SetSplitGreenScreen(bool enabled, int selectedCount)
        {
            try
            {
                if (enabled)
                {
                    SegmentationProperty.ModelType = SegmentationProperty.SegModelGreen;
                    SegmentationProperty.GreenCapacity = 0.5f;
                }
                else
                {
                    SegmentationProperty.ModelType = SegmentationProperty.SegModelAi;
                }

                if (!enabled && selectedCount == 0)
                {
                    RtcEngine.EnableVirtualBackground(false, null, null);
                }
                else
                {
                    RtcEngine.EnableVirtualBackground(true, VirtualBackgroundSource, SegmentationProperty);
                }
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
            }
        }

        public void SetVirtualImgBg(string path)
        {
            try
            {
                VirtualBackgroundSource.BackgroundSourceType = VirtualBackgroundSource.BackgroundImg;
                VirtualBackgroundSource.Source = path;
                RtcEngine.EnableVirtualBackground(true, VirtualBackgroundSource, SegmentationProperty);
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
            }
        }

        public void DisableVirtualBackground()
        {
            try
            {
                VirtualBackgroundSource = new VirtualBackgroundSource();
                RtcEngine.EnableVirtualBackground(false, null, null);
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
            }
        }

        public void SetupBackgroundBlur(float blur)
        {
            try
            {
                VirtualBackgroundSource.BackgroundSourceType = VirtualBackgroundSource.BackgroundBlur;
                VirtualBackgroundSource.Color = 0x000000;
                if (blur >= 100)
                {
                    VirtualBackgroundSource.BlurDegree = VirtualBackgroundSource.BlurDegreeHigh;
                }
                else if (blur >= 50)
                {
                    VirtualBackgroundSource.BlurDegree = VirtualBackgroundSource.BlurDegreeMedium;
                }
                else if (blur >= 0)
                {
                    VirtualBackgroundSource.BlurDegree = VirtualBackgroundSource.BlurDegreeLow;
                }

                RtcEngine.EnableVirtualBackground(true, VirtualBackgroundSource, SegmentationProperty);
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
            }
        }

        public void SetSolidBackground()
        {
            try
            {
                VirtualBackgroundSource = new VirtualBackgroundSource();
                VirtualBackgroundSource.BackgroundSourceType = VirtualBackgroundSource.BackgroundColor;
                VirtualBackgroundSource.Color = 0xfaf1be;
                 
                // Set processing properties for background
                SegmentationProperty = new SegmentationProperty();
                SegmentationProperty.ModelType = SegmentationProperty.SegModelAi;

                // Use SEG_MODEL_GREEN if you have a green background
                SegmentationProperty.GreenCapacity = 0.5f; // Accuracy for identifying green colors (range 0-1)

                // Enable or disable virtual background
                RtcEngine.EnableVirtualBackground(true, VirtualBackgroundSource, SegmentationProperty);
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
            }
        }

        public List<OptionItem> GeneratorOptionItems()
        {
            List<OptionItem> optionItems = new List<OptionItem>
            {
                new OptionItem()
                {
                    Id = IdOriginal,
                    ImgRes = Resource.Drawable.ic_original_image,
                    TitleRes = Resource.String.Lbl_OriginalImage,
                },
                new OptionItem()
                {
                    Id = IdBackgroundBlur,
                    ImgRes = Resource.Drawable.ic_bg_blur,
                    TitleRes = Resource.String.Lbl_Background_Blur,
                },
                new OptionItem()
                {
                    Id = IdBackgroundSolid,
                    ImgRes = Resource.Color.colorCircleYellow,
                    TitleRes = Resource.String.Lbl_Background_Solid,
                },
                new OptionItem()
                {
                    Id = IdSunset,
                    ImgUrl = "https://res.cloudinary.com/dyvrnfxhp/image/upload/v1742294598/sunset_rne6hz.jpg",
                    TitleRes = Resource.String.Lbl_Background_Sunset,
                },
                new OptionItem()
                {
                    Id = IdOffice,
                    ImgUrl = "https://res.cloudinary.com/dyvrnfxhp/image/upload/v1742294598/office_kb0gaq.jpg",
                    TitleRes = Resource.String.Lbl_Background_Office,
                },
                new OptionItem()
                {
                    Id = IdCafe,
                    ImgUrl = "https://res.cloudinary.com/dyvrnfxhp/image/upload/v1742294598/cafe_jd9i4s.jpg",
                    TitleRes = Resource.String.Lbl_Background_Cafe,
                },
                new OptionItem()
                {
                    Id = IdLandscape,
                    ImgUrl = "https://res.cloudinary.com/dyvrnfxhp/image/upload/v1742294598/landscape_mghnyc.jpg",
                    TitleRes = Resource.String.Lbl_Background_Landscape,
                },
                new OptionItem()
                {
                    Id = IdMeetingRoom,
                    ImgUrl = "https://res.cloudinary.com/dyvrnfxhp/image/upload/v1742294598/meeting_room_ro7ifa.jpg",
                    TitleRes = Resource.String.Lbl_Background_Meeting,
                },
                //new OptionItem()
                //{
                //    Id = IdCustomize,
                //    ImgRes = Resource.Drawable.ic_post_img_gallery,
                //    TitleRes = Resource.String.Lbl_Gallery,
                //},
            };

            return optionItems;
        }

        public static async Task<string> GetBackgroundPath(string backgroundUrl)
        {
            try
            {
                var backgroundPath = await WoWonderTools.SaveFileAsync(Methods.Path.FolderDiskFile, backgroundUrl.Split('/').Last(), backgroundUrl, "background");
                return backgroundPath;
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                return null;
            }
        }

    }
}
