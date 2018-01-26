# Movie-Review-Buddy

## Udacity Android Nanodegree Project 1 & 2: Popular Movies
This is a fully functional and colorful android application. The app displays a grid of posters based on popularity, rating, or user favorites (if user has any saved). Details of each movie can be further expanding by tapping on poster thumbnail. Details page includes title, rating, release date, list of clickable movie trailers, and movie reviews.

### Features:
This app can:
* Display a grid of movie posters based on popularity or rating
* Save favorite movies and later display them as a grid
* Allow users to watch movie trailers
* Allow users to read movie reviews

### Concepts Applied
* Model-View-Presenter(MVP) design pattern for organizing code
* Making network calls to 3rd party APIs
* Working with multiple threads
* Parsing JavaScript Object Notation (JSON) data
* Handling and persisting data through different activity lifecycles
* Handling orientation changes

### Technologies Implemented
* SQLite3 for data storage

### Libraries Used
* Picasso
* OkHttp3
* Android Support
	* AppCompat, Constraint, Design, Palette

### How to work with the source code:
This application uses [The Movie Database, TMDb](https://www.themoviedb.org/) API to serve the movie data. You must provide your own API key in order to build the app. After obtaining an API key, add to the "API_KEY" constant in the MainActivity.java file. 

### User Facing Screens
Main Screen </br>
<img src="https://user-images.githubusercontent.com/25759516/32746979-3565dea4-c86c-11e7-8d8f-b37e01083cbe.png" width="400">

Details Screen </br>
<img src="https://user-images.githubusercontent.com/25759516/32746983-389e061e-c86c-11e7-93aa-ba356b5de5cd.png" width="400">

Main Screen + Menu (Landscape) </br>
<img src="https://user-images.githubusercontent.com/25759516/32746985-3ae9c8ae-c86c-11e7-8d1f-6b4913ab98c5.png" width="600">

### Notes:
* Republish of Game of Flicks
	* Update to more appropriate name
	* Uploaded through more proper git and github channels to show proficiency in git commits, push, pulls, etc.

