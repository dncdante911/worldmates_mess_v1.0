using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Android.App;
using Android.Content;
using WoWonder.Helpers.Controller;
using WoWonder.Helpers.Model;
using WoWonder.Helpers.Utils;
using WoWonderClient;
using WoWonderClient.Requests;

namespace WoWonder.SocketSystem
{ 
    [BroadcastReceiver(Enabled = true, Exported = false)]
    [IntentFilter(new[] { Intent.ActionSend })]
    public class ReplyReceiver : BroadcastReceiver
    {
        public override void OnReceive(Context context, Intent intent)
        {
            try
            {
                if (intent?.Action == "ACTION_REPLY")
                {
                    var replyText = AppNotificationsManager.Instance.GetReplyMessage(intent);
                    if (!string.IsNullOrEmpty(replyText))
                    {
                        // Handle the received text, for example, display it in a toast
                        var typeChat = intent.GetStringExtra("TypeChat");
                        var chatId = intent.GetStringExtra("ChatId");
                        var id = intent.GetStringExtra("ToId");
                        var name = intent.GetStringExtra("Name");
                        var avatar = intent.GetStringExtra("Avatar");
                        var color = intent.GetStringExtra("Color");

                        var unixTimestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
                        var time = unixTimestamp.ToString();

                        if (typeChat == "user")
                        {
                            if (Methods.CheckConnectivity())
                            {
                                if (AppSettings.ConnectionTypeChat == InitializeWoWonder.ConnectionType.Socket)
                                {
                                    UserDetails.Socket?.EmitAsync_SendMessage(id, UserDetails.AccessToken, UserDetails.Username, replyText, color, "0", time);
                                }
                                else
                                {
                                    PollyController.RunRetryPolicyFunction(new List<Func<Task>> { () => RequestsAsync.Message.SendMessageAsync(id, time, "", replyText) });
                                }
                            }
                        }
                        else if (typeChat == "group")
                        {
                            if (Methods.CheckConnectivity())
                            {
                                if (AppSettings.ConnectionTypeChat == InitializeWoWonder.ConnectionType.Socket)
                                {
                                    UserDetails.Socket?.EmitAsync_SendGroupMessage(id, UserDetails.AccessToken, UserDetails.Username, replyText, "0", time);
                                }
                                else
                                {
                                    PollyController.RunRetryPolicyFunction(new List<Func<Task>> { () => RequestsAsync.GroupChat.Send_MessageToGroupChatAsync(id, time, replyText) });
                                }
                            }
                        }
                        else if (typeChat == "page")
                        {
                            if (Methods.CheckConnectivity())
                            {
                                if (AppSettings.ConnectionTypeChat == InitializeWoWonder.ConnectionType.Socket)
                                {
                                    UserDetails.Socket?.EmitAsync_SendPageMessage(id, UserDetails.UserId, UserDetails.AccessToken, UserDetails.Username, replyText, "0", time);
                                }
                                else
                                {
                                    PollyController.RunRetryPolicyFunction(new List<Func<Task>> { () => RequestsAsync.PageChat.SendMessageToPageChatAsync(id, UserDetails.UserId, time, replyText) });
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                Methods.DisplayReportResultTrack(e);
            }
        }
    }
} 