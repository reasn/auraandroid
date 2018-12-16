<p align="center">
    <img src="http://getaura.io/img/icon.png" width="200" height="200" alt="Aura Logo" />
</p>

# Aura

With Aura you can show your uncensored thoughts to people around you. It relies on Bluetooth LE and doesn't use internet or wifi.

## Censorship Sucks.
Thoughts are free. Sharing them can be dangerous. 
Nearby Aura users see your thoughts without knowing that it's you. 
It works person-to-person without internet, can't be blocked and leaves no traces with your phone operator.

## How does it work?
* Your Aura is visible to people around you and contains small shareable ideas.
* These ideas can spread from Aura to Aura to change the world.

## Useful links
* Help me change the world by using the [Aura public beta at the mall](https://medium.com/aura-blog/aura-public-beta-at-the-mall-cc6697941cfe).
* Check out [getAura.io](https://www.getAura.io).
* Report or read about [open issues](https://github.com/reasn/auraandroid/issues).

## Contribution

### Code Quality
I'm a big proponent of BDD and maintainable code. Aura is my first Android project in 7 years and
therefore the codebase is not at the level at which I'd like it to be. If this project takes of, it
deserves major refactorings and switching to a test-driven approach.  

### How can I help?
Please get in touch via GitHub or email (info@getAura.io) if you wish to contribute! 

### Deployment (relies on unpublished private credentials)
* Build signed APK (credentials in private Keepass)
* Upload to https://console.cloud.google.com/storage/browser/static.auraapp.io/releases/native/?project=alexthiel-de
* Patch `index.html` in https://github.com/offnet/getaura.io
* Upload to Play console at https://play.google.com/apps/publish/