-- ============================================
-- Скрипт видалення дублікатів таблиць
-- Створено автоматично
-- ============================================

-- ВАЖЛИВО: Перед виконанням створіть бекап БД!
-- mysqldump -u social -p socialhub > backup_$(date +%Y%m%d_%H%M%S).sql

USE socialhub;

-- Група: wo_activities
-- ✅ Залишаємо: wo_activities
-- ❌ Видаляємо: Wo_Activities

-- Група: wo_admininvitations
-- ✅ Залишаємо: wo_admininvitations
-- ❌ Видаляємо: Wo_AdminInvitations

-- Група: wo_ads
-- ✅ Залишаємо: wo_ads
-- ❌ Видаляємо: Wo_Ads

-- Група: wo_affiliates_requests
-- ✅ Залишаємо: wo_affiliates_requests
-- ❌ Видаляємо: Wo_Affiliates_Requests

-- Група: wo_agoravideocall
-- ✅ Залишаємо: wo_agoravideocall
-- ❌ Видаляємо: Wo_AgoraVideoCall

-- Група: wo_albums_media
-- ✅ Залишаємо: wo_albums_media
-- ❌ Видаляємо: Wo_Albums_Media

-- Група: wo_announcement
-- ✅ Залишаємо: wo_announcement
-- ❌ Видаляємо: Wo_Announcement

-- Група: wo_announcement_views
-- ✅ Залишаємо: wo_announcement_views
-- ❌ Видаляємо: Wo_Announcement_Views

-- Група: wo_apps
-- ✅ Залишаємо: wo_apps
-- ❌ Видаляємо: Wo_Apps

-- Група: wo_apps_hash
-- ✅ Залишаємо: wo_apps_hash
-- ❌ Видаляємо: Wo_Apps_Hash

-- Група: wo_apps_permission
-- ✅ Залишаємо: wo_apps_permission
-- ❌ Видаляємо: Wo_Apps_Permission

-- Група: wo_appssessions
-- ✅ Залишаємо: wo_appssessions
-- ❌ Видаляємо: Wo_AppsSessions

-- Група: wo_audiocalls
-- ✅ Залишаємо: wo_audiocalls
-- ❌ Видаляємо: Wo_AudioCalls

-- Група: wo_backup_codes
-- ✅ Залишаємо: wo_backup_codes
-- ❌ Видаляємо: Wo_Backup_Codes

-- Група: wo_bad_login
-- ✅ Залишаємо: wo_bad_login
-- ❌ Видаляємо: Wo_Bad_Login

-- Група: wo_banned_ip
-- ✅ Залишаємо: wo_banned_ip
-- ❌ Видаляємо: Wo_Banned_Ip

-- Група: wo_blocks
-- ✅ Залишаємо: wo_blocks
-- ❌ Видаляємо: Wo_Blocks

-- Група: wo_blog
-- ✅ Залишаємо: wo_blog
-- ❌ Видаляємо: Wo_Blog

-- Група: wo_blog_reaction
-- ✅ Залишаємо: wo_blog_reaction
-- ❌ Видаляємо: Wo_Blog_Reaction

-- Група: wo_blogcommentreplies
-- ✅ Залишаємо: wo_blogcommentreplies
-- ❌ Видаляємо: Wo_BlogCommentReplies

-- Група: wo_blogcomments
-- ✅ Залишаємо: wo_blogcomments
-- ❌ Видаляємо: Wo_BlogComments

-- Група: wo_blogmoviedislikes
-- ✅ Залишаємо: wo_blogmoviedislikes
-- ❌ Видаляємо: Wo_BlogMovieDisLikes

-- Група: wo_blogmovielikes
-- ✅ Залишаємо: wo_blogmovielikes
-- ❌ Видаляємо: Wo_BlogMovieLikes

-- Група: wo_blogs_categories
-- ✅ Залишаємо: wo_blogs_categories
-- ❌ Видаляємо: Wo_Blogs_Categories

-- Група: wo_codes
-- ✅ Залишаємо: wo_codes
-- ❌ Видаляємо: Wo_Codes

-- Група: wo_colored_posts
-- ✅ Залишаємо: wo_colored_posts
-- ❌ Видаляємо: Wo_Colored_Posts

-- Група: wo_comment_replies
-- ✅ Залишаємо: wo_comment_replies
-- ❌ Видаляємо: Wo_Comment_Replies

-- Група: wo_comment_replies_likes
-- ✅ Залишаємо: wo_comment_replies_likes
-- ❌ Видаляємо: Wo_Comment_Replies_Likes

-- Група: wo_comment_replies_wonders
-- ✅ Залишаємо: wo_comment_replies_wonders
-- ❌ Видаляємо: Wo_Comment_Replies_Wonders

-- Група: wo_commentlikes
-- ✅ Залишаємо: wo_commentlikes
-- ❌ Видаляємо: Wo_CommentLikes

-- Група: wo_comments
-- ✅ Залишаємо: wo_comments
-- ❌ Видаляємо: Wo_Comments

-- Група: wo_commentwonders
-- ✅ Залишаємо: wo_commentwonders
-- ❌ Видаляємо: Wo_CommentWonders

-- Група: wo_config
-- ✅ Залишаємо: wo_config
-- ❌ Видаляємо: Wo_Config

-- Група: wo_custom_fields
-- ✅ Залишаємо: wo_custom_fields
-- ❌ Видаляємо: Wo_Custom_Fields

-- Група: wo_custompages
-- ✅ Залишаємо: wo_custompages
-- ❌ Видаляємо: Wo_CustomPages

-- Група: wo_egoing
-- ✅ Залишаємо: wo_egoing
-- ❌ Видаляємо: Wo_Egoing

-- Група: wo_einterested
-- ✅ Залишаємо: wo_einterested
-- ❌ Видаляємо: Wo_Einterested

-- Група: wo_einvited
-- ✅ Залишаємо: wo_einvited
-- ❌ Видаляємо: Wo_Einvited

-- Група: wo_emails
-- ✅ Залишаємо: wo_emails
-- ❌ Видаляємо: Wo_Emails

-- Група: wo_events
-- ✅ Залишаємо: wo_events
-- ❌ Видаляємо: Wo_Events

-- Група: wo_family
-- ✅ Залишаємо: wo_family
-- ❌ Видаляємо: Wo_Family

-- Група: wo_followers
-- ✅ Залишаємо: wo_followers
-- ❌ Видаляємо: Wo_Followers

-- Група: wo_forum_sections
-- ✅ Залишаємо: wo_forum_sections
-- ❌ Видаляємо: Wo_Forum_Sections

-- Група: wo_forum_threads
-- ✅ Залишаємо: wo_forum_threads
-- ❌ Видаляємо: Wo_Forum_Threads

-- Група: wo_forums
-- ✅ Залишаємо: wo_forums
-- ❌ Видаляємо: Wo_Forums

-- Група: wo_forumthreadreplies
-- ✅ Залишаємо: wo_forumthreadreplies
-- ❌ Видаляємо: Wo_ForumThreadReplies

-- Група: wo_funding
-- ✅ Залишаємо: wo_funding
-- ❌ Видаляємо: Wo_Funding

-- Група: wo_funding_raise
-- ✅ Залишаємо: wo_funding_raise
-- ❌ Видаляємо: Wo_Funding_Raise

-- Група: wo_games
-- ✅ Залишаємо: wo_games
-- ❌ Видаляємо: Wo_Games

-- Група: wo_games_players
-- ✅ Залишаємо: wo_games_players
-- ❌ Видаляємо: Wo_Games_Players

-- Група: wo_gender
-- ✅ Залишаємо: wo_gender
-- ❌ Видаляємо: Wo_Gender

-- Група: wo_gifts
-- ✅ Залишаємо: wo_gifts
-- ❌ Видаляємо: Wo_Gifts

-- Група: wo_group_members
-- ✅ Залишаємо: wo_group_members
-- ❌ Видаляємо: Wo_Group_Members

-- Група: wo_groupadmins
-- ✅ Залишаємо: wo_groupadmins
-- ❌ Видаляємо: Wo_GroupAdmins

-- Група: wo_groupchat
-- ✅ Залишаємо: wo_groupchat
-- ❌ Видаляємо: Wo_GroupChat

-- Група: wo_groupchatusers
-- ✅ Залишаємо: wo_groupchatusers
-- ❌ Видаляємо: Wo_GroupChatUsers

-- Група: wo_groups
-- ✅ Залишаємо: wo_groups
-- ❌ Видаляємо: Wo_Groups

-- Група: wo_groups_categories
-- ✅ Залишаємо: wo_groups_categories
-- ❌ Видаляємо: Wo_Groups_Categories

-- Група: wo_hashtags
-- ✅ Залишаємо: wo_hashtags
-- ❌ Видаляємо: Wo_Hashtags

-- Група: wo_hiddenposts
-- ✅ Залишаємо: wo_hiddenposts
-- ❌ Видаляємо: Wo_HiddenPosts

-- Група: wo_invitation_links
-- ✅ Залишаємо: wo_invitation_links
-- ❌ Видаляємо: Wo_Invitation_Links

-- Група: wo_job
-- ✅ Залишаємо: wo_job
-- ❌ Видаляємо: Wo_Job

-- Група: wo_job_apply
-- ✅ Залишаємо: wo_job_apply
-- ❌ Видаляємо: Wo_Job_Apply

-- Група: wo_job_categories
-- ✅ Залишаємо: wo_job_categories
-- ❌ Видаляємо: Wo_Job_Categories

-- Група: wo_langs
-- ✅ Залишаємо: wo_langs
-- ❌ Видаляємо: Wo_Langs

-- Група: wo_likes
-- ✅ Залишаємо: wo_likes
-- ❌ Видаляємо: Wo_Likes

-- Група: wo_live_sub_users
-- ✅ Залишаємо: wo_live_sub_users
-- ❌ Видаляємо: Wo_Live_Sub_Users

-- Група: wo_manage_pro
-- ✅ Залишаємо: wo_manage_pro
-- ❌ Видаляємо: Wo_Manage_Pro

-- Група: wo_messages
-- ✅ Залишаємо: wo_messages
-- ❌ Видаляємо: Wo_Messages

-- Група: wo_monetizationsubscription
-- ✅ Залишаємо: wo_monetizationsubscription
-- ❌ Видаляємо: Wo_MonetizationSubscription

-- Група: wo_moviecommentreplies
-- ✅ Залишаємо: wo_moviecommentreplies
-- ❌ Видаляємо: Wo_MovieCommentReplies

-- Група: wo_moviecomments
-- ✅ Залишаємо: wo_moviecomments
-- ❌ Видаляємо: Wo_MovieComments

-- Група: wo_movies
-- ✅ Залишаємо: wo_movies
-- ❌ Видаляємо: Wo_Movies

-- Група: wo_notifications
-- ✅ Залишаємо: wo_notifications
-- ❌ Видаляємо: Wo_Notifications

-- Група: wo_offers
-- ✅ Залишаємо: wo_offers
-- ❌ Видаляємо: Wo_Offers

-- Група: wo_pageadmins
-- ✅ Залишаємо: wo_pageadmins
-- ❌ Видаляємо: Wo_PageAdmins

-- Група: wo_pagerating
-- ✅ Залишаємо: wo_pagerating
-- ❌ Видаляємо: Wo_PageRating

-- Група: wo_pages
-- ✅ Залишаємо: wo_pages
-- ❌ Видаляємо: Wo_Pages

-- Група: wo_pages_categories
-- ✅ Залишаємо: wo_pages_categories
-- ❌ Видаляємо: Wo_Pages_Categories

-- Група: wo_pages_invites
-- ✅ Залишаємо: wo_pages_invites
-- ❌ Видаляємо: Wo_Pages_Invites

-- Група: wo_pages_likes
-- ✅ Залишаємо: wo_pages_likes
-- ❌ Видаляємо: Wo_Pages_Likes

-- Група: wo_payment_transactions
-- ✅ Залишаємо: wo_payment_transactions
-- ❌ Видаляємо: Wo_Payment_Transactions

-- Група: wo_payments
-- ✅ Залишаємо: wo_payments
-- ❌ Видаляємо: Wo_Payments

-- Група: wo_pinnedposts
-- ✅ Залишаємо: wo_pinnedposts
-- ❌ Видаляємо: Wo_PinnedPosts

-- Група: wo_pokes
-- ✅ Залишаємо: wo_pokes
-- ❌ Видаляємо: Wo_Pokes

-- Група: wo_posts
-- ✅ Залишаємо: wo_posts
-- ❌ Видаляємо: Wo_Posts

-- Група: wo_productreview
-- ✅ Залишаємо: wo_productreview
-- ❌ Видаляємо: Wo_ProductReview

-- Група: wo_products
-- ✅ Залишаємо: wo_products
-- ❌ Видаляємо: Wo_Products

-- Група: wo_products_categories
-- ✅ Залишаємо: wo_products_categories
-- ❌ Видаляємо: Wo_Products_Categories

-- Група: wo_products_media
-- ✅ Залишаємо: wo_products_media
-- ❌ Видаляємо: Wo_Products_Media

-- Група: wo_profilefields
-- ✅ Залишаємо: wo_profilefields
-- ❌ Видаляємо: Wo_ProfileFields

-- Група: wo_purchases
-- ✅ Залишаємо: wo_purchases
-- ❌ Видаляємо: Wo_Purchases

-- Група: wo_reactions
-- ✅ Залишаємо: wo_reactions
-- ❌ Видаляємо: Wo_Reactions

-- Група: wo_reactions_types
-- ✅ Залишаємо: wo_reactions_types
-- ❌ Видаляємо: Wo_Reactions_Types

-- Група: wo_recentsearches
-- ✅ Залишаємо: wo_recentsearches
-- ❌ Видаляємо: Wo_RecentSearches

-- Група: wo_refund
-- ✅ Залишаємо: wo_refund
-- ❌ Видаляємо: Wo_Refund

-- Група: wo_relationship
-- ✅ Залишаємо: wo_relationship
-- ❌ Видаляємо: Wo_Relationship

-- Група: wo_reports
-- ✅ Залишаємо: wo_reports
-- ❌ Видаляємо: Wo_Reports

-- Група: wo_savedposts
-- ✅ Залишаємо: wo_savedposts
-- ❌ Видаляємо: Wo_SavedPosts

-- Група: wo_stickers
-- ✅ Залишаємо: wo_stickers
-- ❌ Видаляємо: Wo_Stickers

-- Група: wo_story_seen
-- ✅ Залишаємо: wo_story_seen
-- ❌ Видаляємо: Wo_Story_Seen

-- Група: wo_storycomments
-- ✅ Залишаємо: wo_storycomments
-- ❌ Видаляємо: Wo_StoryComments

-- Група: wo_storyreactions
-- ✅ Залишаємо: wo_storyreactions
-- ❌ Видаляємо: Wo_StoryReactions

-- Група: wo_sub_categories
-- ✅ Залишаємо: wo_sub_categories
-- ❌ Видаляємо: Wo_Sub_Categories

-- Група: wo_terms
-- ✅ Залишаємо: wo_terms
-- ❌ Видаляємо: Wo_Terms

-- Група: wo_tokens
-- ✅ Залишаємо: wo_tokens
-- ❌ Видаляємо: Wo_Tokens

-- Група: wo_user_gifts
-- ✅ Залишаємо: wo_user_gifts
-- ❌ Видаляємо: Wo_User_Gifts

-- Група: wo_useraddress
-- ✅ Залишаємо: wo_useraddress
-- ❌ Видаляємо: Wo_UserAddress

-- Група: wo_userads_data
-- ✅ Залишаємо: wo_userads_data
-- ❌ Видаляємо: Wo_UserAds_Data

-- Група: wo_usercard
-- ✅ Залишаємо: wo_usercard
-- ❌ Видаляємо: Wo_UserCard

-- Група: wo_userfields
-- ✅ Залишаємо: wo_userfields
-- ❌ Видаляємо: Wo_UserFields

-- Група: wo_usermonetization
-- ✅ Залишаємо: wo_usermonetization
-- ❌ Видаляємо: Wo_UserMonetization

-- Група: wo_userorders
-- ✅ Залишаємо: wo_userorders
-- ❌ Видаляємо: Wo_UserOrders

-- Група: wo_userschat
-- ✅ Залишаємо: wo_userschat
-- ❌ Видаляємо: Wo_UsersChat

-- Група: wo_userstorymedia
-- ✅ Залишаємо: wo_userstorymedia
-- ❌ Видаляємо: Wo_UserStoryMedia

-- Група: wo_verification_requests
-- ✅ Залишаємо: wo_verification_requests
-- ❌ Видаляємо: Wo_Verification_Requests

-- Група: wo_videocalles
-- ✅ Залишаємо: wo_videocalles
-- ❌ Видаляємо: Wo_VideoCalles

-- Група: wo_votes
-- ✅ Залишаємо: wo_votes
-- ❌ Видаляємо: Wo_Votes

-- Група: wo_wonders
-- ✅ Залишаємо: wo_wonders
-- ❌ Видаляємо: Wo_Wonders

-- ============================================
-- DROP TABLE команди
-- ============================================

DROP TABLE IF EXISTS `Wo_Activities`;
DROP TABLE IF EXISTS `Wo_AdminInvitations`;
DROP TABLE IF EXISTS `Wo_Ads`;
DROP TABLE IF EXISTS `Wo_Affiliates_Requests`;
DROP TABLE IF EXISTS `Wo_AgoraVideoCall`;
DROP TABLE IF EXISTS `Wo_Albums_Media`;
DROP TABLE IF EXISTS `Wo_Announcement`;
DROP TABLE IF EXISTS `Wo_Announcement_Views`;
DROP TABLE IF EXISTS `Wo_Apps`;
DROP TABLE IF EXISTS `Wo_AppsSessions`;
DROP TABLE IF EXISTS `Wo_Apps_Hash`;
DROP TABLE IF EXISTS `Wo_Apps_Permission`;
DROP TABLE IF EXISTS `Wo_AudioCalls`;
DROP TABLE IF EXISTS `Wo_Backup_Codes`;
DROP TABLE IF EXISTS `Wo_Bad_Login`;
DROP TABLE IF EXISTS `Wo_Banned_Ip`;
DROP TABLE IF EXISTS `Wo_Blocks`;
DROP TABLE IF EXISTS `Wo_Blog`;
DROP TABLE IF EXISTS `Wo_BlogCommentReplies`;
DROP TABLE IF EXISTS `Wo_BlogComments`;
DROP TABLE IF EXISTS `Wo_BlogMovieDisLikes`;
DROP TABLE IF EXISTS `Wo_BlogMovieLikes`;
DROP TABLE IF EXISTS `Wo_Blog_Reaction`;
DROP TABLE IF EXISTS `Wo_Blogs_Categories`;
DROP TABLE IF EXISTS `Wo_Codes`;
DROP TABLE IF EXISTS `Wo_Colored_Posts`;
DROP TABLE IF EXISTS `Wo_CommentLikes`;
DROP TABLE IF EXISTS `Wo_CommentWonders`;
DROP TABLE IF EXISTS `Wo_Comment_Replies`;
DROP TABLE IF EXISTS `Wo_Comment_Replies_Likes`;
DROP TABLE IF EXISTS `Wo_Comment_Replies_Wonders`;
DROP TABLE IF EXISTS `Wo_Comments`;
DROP TABLE IF EXISTS `Wo_Config`;
DROP TABLE IF EXISTS `Wo_CustomPages`;
DROP TABLE IF EXISTS `Wo_Custom_Fields`;
DROP TABLE IF EXISTS `Wo_Egoing`;
DROP TABLE IF EXISTS `Wo_Einterested`;
DROP TABLE IF EXISTS `Wo_Einvited`;
DROP TABLE IF EXISTS `Wo_Emails`;
DROP TABLE IF EXISTS `Wo_Events`;
DROP TABLE IF EXISTS `Wo_Family`;
DROP TABLE IF EXISTS `Wo_Followers`;
DROP TABLE IF EXISTS `Wo_ForumThreadReplies`;
DROP TABLE IF EXISTS `Wo_Forum_Sections`;
DROP TABLE IF EXISTS `Wo_Forum_Threads`;
DROP TABLE IF EXISTS `Wo_Forums`;
DROP TABLE IF EXISTS `Wo_Funding`;
DROP TABLE IF EXISTS `Wo_Funding_Raise`;
DROP TABLE IF EXISTS `Wo_Games`;
DROP TABLE IF EXISTS `Wo_Games_Players`;
DROP TABLE IF EXISTS `Wo_Gender`;
DROP TABLE IF EXISTS `Wo_Gifts`;
DROP TABLE IF EXISTS `Wo_GroupAdmins`;
DROP TABLE IF EXISTS `Wo_GroupChat`;
DROP TABLE IF EXISTS `Wo_GroupChatUsers`;
DROP TABLE IF EXISTS `Wo_Group_Members`;
DROP TABLE IF EXISTS `Wo_Groups`;
DROP TABLE IF EXISTS `Wo_Groups_Categories`;
DROP TABLE IF EXISTS `Wo_Hashtags`;
DROP TABLE IF EXISTS `Wo_HiddenPosts`;
DROP TABLE IF EXISTS `Wo_Invitation_Links`;
DROP TABLE IF EXISTS `Wo_Job`;
DROP TABLE IF EXISTS `Wo_Job_Apply`;
DROP TABLE IF EXISTS `Wo_Job_Categories`;
DROP TABLE IF EXISTS `Wo_Langs`;
DROP TABLE IF EXISTS `Wo_Likes`;
DROP TABLE IF EXISTS `Wo_Live_Sub_Users`;
DROP TABLE IF EXISTS `Wo_Manage_Pro`;
DROP TABLE IF EXISTS `Wo_Messages`;
DROP TABLE IF EXISTS `Wo_MonetizationSubscription`;
DROP TABLE IF EXISTS `Wo_MovieCommentReplies`;
DROP TABLE IF EXISTS `Wo_MovieComments`;
DROP TABLE IF EXISTS `Wo_Movies`;
DROP TABLE IF EXISTS `Wo_Notifications`;
DROP TABLE IF EXISTS `Wo_Offers`;
DROP TABLE IF EXISTS `Wo_PageAdmins`;
DROP TABLE IF EXISTS `Wo_PageRating`;
DROP TABLE IF EXISTS `Wo_Pages`;
DROP TABLE IF EXISTS `Wo_Pages_Categories`;
DROP TABLE IF EXISTS `Wo_Pages_Invites`;
DROP TABLE IF EXISTS `Wo_Pages_Likes`;
DROP TABLE IF EXISTS `Wo_Payment_Transactions`;
DROP TABLE IF EXISTS `Wo_Payments`;
DROP TABLE IF EXISTS `Wo_PinnedPosts`;
DROP TABLE IF EXISTS `Wo_Pokes`;
DROP TABLE IF EXISTS `Wo_Posts`;
DROP TABLE IF EXISTS `Wo_ProductReview`;
DROP TABLE IF EXISTS `Wo_Products`;
DROP TABLE IF EXISTS `Wo_Products_Categories`;
DROP TABLE IF EXISTS `Wo_Products_Media`;
DROP TABLE IF EXISTS `Wo_ProfileFields`;
DROP TABLE IF EXISTS `Wo_Purchases`;
DROP TABLE IF EXISTS `Wo_Reactions`;
DROP TABLE IF EXISTS `Wo_Reactions_Types`;
DROP TABLE IF EXISTS `Wo_RecentSearches`;
DROP TABLE IF EXISTS `Wo_Refund`;
DROP TABLE IF EXISTS `Wo_Relationship`;
DROP TABLE IF EXISTS `Wo_Reports`;
DROP TABLE IF EXISTS `Wo_SavedPosts`;
DROP TABLE IF EXISTS `Wo_Stickers`;
DROP TABLE IF EXISTS `Wo_StoryComments`;
DROP TABLE IF EXISTS `Wo_StoryReactions`;
DROP TABLE IF EXISTS `Wo_Story_Seen`;
DROP TABLE IF EXISTS `Wo_Sub_Categories`;
DROP TABLE IF EXISTS `Wo_Terms`;
DROP TABLE IF EXISTS `Wo_Tokens`;
DROP TABLE IF EXISTS `Wo_UserAddress`;
DROP TABLE IF EXISTS `Wo_UserAds_Data`;
DROP TABLE IF EXISTS `Wo_UserCard`;
DROP TABLE IF EXISTS `Wo_UserFields`;
DROP TABLE IF EXISTS `Wo_UserMonetization`;
DROP TABLE IF EXISTS `Wo_UserOrders`;
DROP TABLE IF EXISTS `Wo_UserStoryMedia`;
DROP TABLE IF EXISTS `Wo_User_Gifts`;
DROP TABLE IF EXISTS `Wo_UsersChat`;
DROP TABLE IF EXISTS `Wo_Verification_Requests`;
DROP TABLE IF EXISTS `Wo_VideoCalles`;
DROP TABLE IF EXISTS `Wo_Votes`;
DROP TABLE IF EXISTS `Wo_Wonders`;

-- Всього таблиць для видалення: 119
