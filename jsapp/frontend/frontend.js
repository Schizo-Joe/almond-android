
/**
 * Module dependencies.
 */

const Q = require('q');

const express = require('express');
const path = require('path');
const logger = require('morgan');
const cookieParser = require('cookie-parser');
const bodyParser = require('body-parser');
const session = require('express-session');
const errorHandler = require('errorhandler');
const csurf = require('csurf');
const connect_flash = require('connect-flash');
const expressWs = require('express-ws');

function Frontend() {
    this._init.apply(this, arguments);
}

Frontend.prototype._init = function _init() {
    // all environments
    this._app = express();
    expressWs(this._app);

    this._app.set('port', process.env.PORT || 3000);
    this._app.set('views', path.join(__dirname, 'views'));
    this._app.set('view engine', 'jade');
    this._app.use(logger('dev'));
    this._app.use(bodyParser.json());
    this._app.use(bodyParser.urlencoded({ extended: true }));
    this._app.use(cookieParser());
    this._app.use(session({ resave: false,
                            saveUninitialized: false,
                            secret: 'badgersbadgersbadgers' }));
    this._app.use(connect_flash());
    this._app.use(express.static(path.join(__dirname, 'public')));
    this._app.use(csurf());

    // development only
    if ('development' == this._app.get('env')) {
        console.log('Frontend initialized in development mode');
        this._app.use(errorHandler());
    }

    this._app.use('/', require('./routes/index'));
    this._app.use('/apps', require('./routes/apps'));
    this._app.use('/devices', require('./routes/devices'));
    require('./routes/assistant')(this._app);
}

var server = null;

Frontend.prototype.open = function() {
    return Q.ninvoke(this._app, 'listen', this._app.get('port'), '127.0.0.1')
        .then(function() {
            console.log('Express server listening on port ' + this._app.get('port'));
        }.bind(this));
};

Frontend.prototype.close = function() {
    return Q.ninvoke(server, 'close').then(function() {
        console.log('Express server stopped');
    }).catch(function(error) {
        console.log('Error stopping Express server: ' + error);
        console.log(error.stack);
    });
};

Frontend.prototype.getApp = function() {
    return this._app;
};

Frontend.prototype.setEngine = function(engine) {
    this._app.engine = engine;
};

module.exports = Frontend;
