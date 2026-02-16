const moment = require("moment");
var fs = require('fs');
var express = require('express');
var app = express();
const path = require('path');

let ctx = {};

// var http = require('http').createServer(app);
// var io = require('socket.io')(http);
const configFile = require("./config.json")
const { Sequelize, Op, DataTypes } = require("sequelize");

// const notificationTemplate = Handlebars.compile(notification.toString());

const listeners = require('./listeners/listeners')
const turnHelper = require('./helpers/turn-credentials')

let serverPort
let server
let io

async function loadConfig(ctx) {
  let config = await ctx.wo_config.findAll({ raw: true })
  for (let c of config) {
    ctx.globalconfig[c.name] = c.value
  }
  ctx.globalconfig["site_url"] = configFile.site_url
  ctx.globalconfig['theme_url'] = ctx.globalconfig["site_url"] + '/themes/' + ctx.globalconfig['theme']

  ctx.globalconfig["s3_site_url"]         = "https://test.s3.amazonaws.com";
  if (ctx.globalconfig["bucket_name"] && ctx.globalconfig["bucket_name"] != '') {
      ctx.globalconfig["s3_site_url"] = "https://"+ctx.globalconfig["bucket_name"]+".s3.amazonaws.com";
  }
  ctx.globalconfig["s3_site_url_2"]          = "https://test.s3.amazonaws.com";
  if (ctx.globalconfig["bucket_name_2"] && ctx.globalconfig["bucket_name_2"] != '') {
      ctx.globalconfig["s3_site_url_2"] = "https://"+ctx.globalconfig["bucket_name_2"]+".s3.amazonaws.com";
  }
  var endpoint_url = ctx.globalconfig['ftp_endpoint']; 
  ctx.globalconfig['ftp_endpoint'] = endpoint_url.replace('https://', '');

   if (ctx.globalconfig["redis"] === "Y") {
     try {
       const redisAdapter = require('socket.io-redis');
       const redisHost = ctx.globalconfig["redis_host"] || 'localhost';
       const redisPort = ctx.globalconfig["redis_port"] || 6379;
       const redisPassword = ctx.globalconfig["redis_password"] || '';
       const adapterOpts = { host: redisHost, port: redisPort };
       if (redisPassword) {
         adapterOpts.auth_pass = redisPassword;
       }
       io.adapter(redisAdapter(adapterOpts));
       console.log(`[Redis Adapter] Connected to ${redisHost}:${redisPort}`);
     } catch (e) {
       console.warn('[Redis Adapter] Failed to initialize:', e.message);
     }
   }


  if (ctx.globalconfig["nodejs_ssl"] == 1) {
    var https = require('https');
    var options = {
      key: fs.readFileSync(path.resolve(__dirname, ctx.globalconfig["nodejs_key_path"])),
      cert: fs.readFileSync(path.resolve(__dirname, ctx.globalconfig["nodejs_cert_path"]))
    };
    serverPort = ctx.globalconfig["nodejs_ssl_port"];
    server = https.createServer(options, app);
  } else {
    serverPort = ctx.globalconfig["nodejs_port"];
    server = require('http').createServer(app);
  }

}


async function loadLangs(ctx) {
  let langs = await ctx.wo_langs.findAll({ raw: true })
  for (let c of langs) {
    ctx.globallangs[c.lang_key] = c.english
  }
}


async function init() {
  var sequelize = new Sequelize(configFile.sql_db_name, configFile.sql_db_user, configFile.sql_db_pass, {
    host: configFile.sql_db_host,
    dialect: "mysql",
    logging: function () {},
    pool: {
        max: 20,
        min: 0,
        idle: 10000
    }
  });



  // Helper: safely load a model, log warning if not found
  function loadModel(name) {
    try {
      return require(`./models/${name}`)(sequelize, DataTypes);
    } catch (e) {
      console.warn(`[WARN] Model ${name} not found, skipping: ${e.message}`);
      return null;
    }
  }

  // Core models (required)
  ctx.wo_messages = require("./models/wo_messages")(sequelize, DataTypes)
  ctx.wo_userschat = require("./models/wo_userschat")(sequelize, DataTypes)
  ctx.wo_users = require("./models/wo_users")(sequelize, DataTypes)
  ctx.wo_notification = require("./models/wo_notifications")(sequelize, DataTypes)
  ctx.wo_groupchat = require("./models/wo_groupchat")(sequelize, DataTypes)
  ctx.wo_groupchatusers = require("./models/wo_groupchatusers")(sequelize, DataTypes)
  ctx.wo_appssessions = require("./models/wo_appssessions")(sequelize, DataTypes)
  ctx.wo_langs = require("./models/wo_langs")(sequelize, DataTypes)
  ctx.wo_config = require("./models/wo_config")(sequelize, DataTypes)
  ctx.wo_blocks = require("./models/wo_blocks")(sequelize, DataTypes)
  ctx.wo_mute = require("./models/wo_mute")(sequelize, DataTypes)

  // Optional models (won't crash if missing on server)
  ctx.wo_videocalls = loadModel("wo_videocalles")
  ctx.wo_audiocalls = loadModel("wo_audiocalls")
  ctx.wo_followers = loadModel("wo_followers")
  ctx.wo_hashtags = loadModel("wo_hashtags")
  ctx.wo_posts = loadModel("wo_posts")
  ctx.wo_comments = loadModel("wo_comments")
  ctx.wo_comment_replies = loadModel("wo_comment_replies")
  ctx.wo_pages = loadModel("wo_pages")
  ctx.wo_groups = loadModel("wo_groups")
  ctx.wo_events = loadModel("wo_events")
  ctx.wo_userstory = loadModel("wo_userstory")
  ctx.wo_reactions_types = loadModel("wo_reactions_types")
  ctx.wo_reactions = loadModel("wo_reactions")
  ctx.wo_blog_reaction = loadModel("wo_blog_reaction")
  ctx.wo_calls = loadModel("wo_calls")
  ctx.wo_group_calls = loadModel("wo_group_calls")
  ctx.wo_group_call_participants = loadModel("wo_group_call_participants")
  ctx.wo_ice_candidates = loadModel("wo_ice_candidates")
  ctx.wo_call_statistics = loadModel("wo_call_statistics")

  ctx.globalconfig = {}
  ctx.globallangs = {}
  ctx.socketIdUserHash = {}
  ctx.userHashUserId = {}
  ctx.userIdCount = {}
  ctx.userIdChatOpen = {}
  ctx.userIdSocket = {}  // ✅ ВИПРАВЛЕНО: Має бути ОБ'ЄКТ, не масив!
  ctx.userIdExtra = {}
  ctx.userIdGroupChatOpen = {}

  await loadConfig(ctx)
  await loadLangs(ctx)

}


async function main() {
  await init()

  // ==================== REST API для TURN/ICE ====================
  // turnHelper вже імпортовано на початку файлу

  // Middleware для парсинга JSON
  app.use(express.json());

  // GET /api/ice-servers/:userId - получить ICE серверы с TURN credentials
  app.get('/api/ice-servers/:userId', (req, res) => {
    try {
      const userId = req.params.userId;

      if (!userId) {
        return res.status(400).json({
          success: false,
          error: 'userId is required'
        });
      }

      const iceConfig = turnHelper.getIceConfigForAndroid(userId);
      res.json(iceConfig);

      console.log(`[ICE] Generated ICE servers for user ${userId}`);
    } catch (error) {
      console.error('[ICE] Error generating ICE servers:', error);
      res.status(500).json({
        success: false,
        error: 'Failed to generate ICE servers'
      });
    }
  });

  // POST /api/turn-credentials - альтернативный метод
  app.post('/api/turn-credentials', (req, res) => {
    try {
      const { userId, ttl } = req.body;

      if (!userId) {
        return res.status(400).json({
          success: false,
          error: 'userId is required'
        });
      }

      const credentials = turnHelper.generateTurnCredentials(userId, ttl || 86400);
      const iceServers = turnHelper.getIceServers(userId, ttl || 86400);

      res.json({
        success: true,
        credentials: credentials,
        iceServers: iceServers
      });

      console.log(`[TURN] Generated credentials for user ${userId}`);
    } catch (error) {
      console.error('[TURN] Error generating credentials:', error);
      res.status(500).json({
        success: false,
        error: 'Failed to generate credentials'
      });
    }
  });

  // Health check endpoint
  app.get('/api/health', (req, res) => {
    res.json({
      status: 'ok',
      timestamp: new Date().toISOString(),
      uptime: process.uptime()
    });
  });

  app.get('/', (req, res) => {
    res.sendFile(__dirname + '/index.html');
  });
  io = require('socket.io')(server, {
    allowEIO3: true,
    cors: {
        origin: true,
        credentials: true
    },
    transports: ['websocket', 'polling'], // WebSocket first, polling as fallback
    pingTimeout: 60000,
    pingInterval: 25000,
  });

  io.on('connection', async (socket, query) => {
    await listeners.registerListeners(socket, io, ctx)
  })

  server.listen(serverPort, function() {
    console.log('server up and running at %s port', serverPort);
  });
}

main()
