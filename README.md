# grooob.com

## Vision
Planning as rough as possible.
Less Details. Less data. Less time.
Together simultaneously as Team.
View everything in Realtime.

## TODOs / Features / Releases
- a data model for user management, company management
- a data model for domain-model: project, task, capacity, ...
- a functional core for all business logic
- authorization and admin role
- invitation
- user administration (pending, guest, ...)
- resend password

## technical decisions
- clojure
- server with datahike
- Client: re-frame
- model, logic and spec in cljc in order to use at client and server
- EITHER: drawing in a canvas: reactive client with some rx-tool
- OR: drawing svg in re-frame


## development mode
```
npm i react
npm i react-dom
npm i shadow-cljs --save-dev
npm install node-sass --save-dev
npm install bulma --save-dev
```

package.json should look like this
```
{
  "devDependencies": {
    "bulma": "^0.9.4",
    "node-sass": "^8.0.0",
    "shadow-cljs": "^2.16.5"
  },
  "dependencies": {
    "react": "^17.0.2",
    "react-dom": "^17.0.2"
  },
  "scripts": {
    "css-build": "node-sass --omit-source-map-url resources/scss/screen.scss resources/public/css/screen.css",
    "css-watch": "npm run css-build -- --watch",
    "start": "npm run css-watch"
  }
}
```

## Running

watch and compile sass
```
npm run start
```

watch and compile shadow-cljs
```
shadow-cljs watch app
```

run server in repl
```
(start)
(stop)
(restart)
```

run client in browser
```
localhost:3000
```

test api in swagger
```
localhost:3000/swagger-ui
```

