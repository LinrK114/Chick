using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media.Animation;
using System;
using System.IO;
using Windows.Media.Core;
using Windows.Media.Playback;

namespace Chick
{
    public sealed partial class MainWindow : Window
    {
        private MediaPlayer? _mediaPlayer;

        public MainWindow()
        {
            this.InitializeComponent();

            // 无标题栏设置
            var appWindow = this.AppWindow;
            if (appWindow != null)
            {
                appWindow.TitleBar.ExtendsContentIntoTitleBar = true;
                appWindow.TitleBar.ButtonBackgroundColor = Microsoft.UI.Colors.Transparent;
                appWindow.TitleBar.ButtonInactiveBackgroundColor = Microsoft.UI.Colors.Transparent;
                appWindow.TitleBar.ButtonForegroundColor = Microsoft.UI.Colors.Black;
                appWindow.TitleBar.ButtonHoverBackgroundColor = Microsoft.UI.Colors.LightGray;
                appWindow.TitleBar.ButtonPressedBackgroundColor = Microsoft.UI.Colors.Gray;
            }

            InitializeSound();
        }

        private async void InitializeSound()
        {
            try
            {
                string exeFolder = Path.GetDirectoryName(System.Reflection.Assembly.GetExecutingAssembly().Location);
                string audioPath = Path.Combine(exeFolder, "Assets", "click_sound.wav");

                if (File.Exists(audioPath))
                {
                    var file = await Windows.Storage.StorageFile.GetFileFromPathAsync(audioPath);
                    _mediaPlayer = new MediaPlayer();
                    _mediaPlayer.Source = MediaSource.CreateFromStorageFile(file);
                    _mediaPlayer.Volume = 1.0;
                }
                else
                {
                    _mediaPlayer = null;
                }
            }
            catch
            {
                _mediaPlayer = null;
            }
        }

        private void CenterImage_Tapped(object sender, TappedRoutedEventArgs e)
        {
            PlayScaleAnimation();

            if (_mediaPlayer != null)
            {
                _mediaPlayer.PlaybackSession.Position = TimeSpan.Zero;
                _mediaPlayer.Play();
            }
        }

        private void PlayScaleAnimation()
        {
            var animationX = new DoubleAnimationUsingKeyFrames();
            animationX.KeyFrames.Add(new EasingDoubleKeyFrame { KeyTime = KeyTime.FromTimeSpan(TimeSpan.FromMilliseconds(0)), Value = 1.0 });
            animationX.KeyFrames.Add(new EasingDoubleKeyFrame { KeyTime = KeyTime.FromTimeSpan(TimeSpan.FromMilliseconds(80)), Value = 0.85 });
            animationX.KeyFrames.Add(new EasingDoubleKeyFrame { KeyTime = KeyTime.FromTimeSpan(TimeSpan.FromMilliseconds(160)), Value = 1.0 });
            Storyboard.SetTarget(animationX, ImageScale);
            Storyboard.SetTargetProperty(animationX, "ScaleX");

            var animationY = new DoubleAnimationUsingKeyFrames();
            animationY.KeyFrames.Add(new EasingDoubleKeyFrame { KeyTime = KeyTime.FromTimeSpan(TimeSpan.FromMilliseconds(0)), Value = 1.0 });
            animationY.KeyFrames.Add(new EasingDoubleKeyFrame { KeyTime = KeyTime.FromTimeSpan(TimeSpan.FromMilliseconds(80)), Value = 0.85 });
            animationY.KeyFrames.Add(new EasingDoubleKeyFrame { KeyTime = KeyTime.FromTimeSpan(TimeSpan.FromMilliseconds(160)), Value = 1.0 });
            Storyboard.SetTarget(animationY, ImageScale);
            Storyboard.SetTargetProperty(animationY, "ScaleY");

            var storyboard = new Storyboard();
            storyboard.Children.Add(animationX);
            storyboard.Children.Add(animationY);
            storyboard.Begin();
        }
    }
}