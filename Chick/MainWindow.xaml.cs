using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media.Animation;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using Windows.Media.Core;
using Windows.Media.Playback;

namespace Chick   // 如果你的命名空间不同，请改成你的实际命名空间
{
    public sealed partial class MainWindow : Window
    {
        private List<MediaPlayer> _mediaPlayers = new List<MediaPlayer>();
        private int _maxConcurrentSounds = 50;   // 同时播放的最大数量，可自行修改
        private SemaphoreSlim _semaphore;

        public MainWindow()
        {
            this.InitializeComponent();

            // 设置无标题栏（扩展内容到标题栏，并设置按钮透明）
            var appWindow = this.AppWindow;
            if (appWindow != null)
            {
                appWindow.TitleBar.ExtendsContentIntoTitleBar = true;
                appWindow.TitleBar.ButtonBackgroundColor = Microsoft.UI.Colors.Transparent;
                appWindow.TitleBar.ButtonInactiveBackgroundColor = Microsoft.UI.Colors.Transparent;
                appWindow.TitleBar.ButtonForegroundColor = Microsoft.UI.Colors.White;
                appWindow.TitleBar.ButtonHoverBackgroundColor = Microsoft.UI.Colors.DarkGray;
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

                if (!File.Exists(audioPath))
                {
                    _semaphore = null;
                    return;
                }

                var file = await Windows.Storage.StorageFile.GetFileFromPathAsync(audioPath);
                _semaphore = new SemaphoreSlim(_maxConcurrentSounds, _maxConcurrentSounds);

                for (int i = 0; i < _maxConcurrentSounds; i++)
                {
                    var player = new MediaPlayer();
                    player.Source = MediaSource.CreateFromStorageFile(file);
                    player.Volume = 1.0;
                    player.MediaEnded += (sender, args) => _semaphore?.Release();
                    _mediaPlayers.Add(player);
                }
            }
            catch
            {
                _semaphore = null;
            }
        }

        private void CenterImage_Tapped(object sender, TappedRoutedEventArgs e)
        {
            PlayScaleAnimation();
            PlayClickSound();
        }

        private void PlayClickSound()
        {
            if (_semaphore == null) return;
            if (!_semaphore.Wait(0)) return;   // 达到最大并发数，直接放弃本次播放

            MediaPlayer player = _mediaPlayers.FirstOrDefault(p => p.PlaybackSession.PlaybackState != MediaPlaybackState.Playing);
            if (player == null)
            {
                _semaphore.Release();
                return;
            }

            player.PlaybackSession.Position = TimeSpan.Zero;
            player.Play();
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
