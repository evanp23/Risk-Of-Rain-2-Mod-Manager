<div id="top"></div>
<!--
*** Thanks for checking out the Best-README-Template. If you have a suggestion
*** that would make this better, please fork the repo and create a pull request
*** or simply open an issue with the tag "enhancement".
*** Don't forget to give the project a star!
*** Thanks again! Now go create something AMAZING! :D
-->



<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![MIT License][license-shield]][license-url]
[![LinkedIn][linkedin-shield]][linkedin-url]



<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/evanp23/Risk-Of-Rain-2-Mod-Manager">
    Logo Coming soon!
    <!-- <img src="images/logo.png" alt="Logo" width="80" height="80"> -->
  </a>

  <h3 align="center">Risk Of Rain 2 Mod Manager</h3>

  <p align="center">
    Mod manager for ROR2 Thunderstore Mods, built in JavaFX.
    <br />
    <a href="https://github.com/evanp23/Risk-Of-Rain-2-Mod-Manager"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="https://github.com/evanp23/Risk-Of-Rain-2-Mod-Manager">View Demo</a>
    ·
    <a href="https://github.com/evanp23/Risk-Of-Rain-2-Mod-Manager/issues">Report Bug</a>
    ·
    <a href="https://github.com/evanp23/Risk-Of-Rain-2-Mod-Manager/issues">Request Feature</a>
  </p>
</div>



<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>



<!-- ABOUT THE PROJECT -->
## About The Project

[![Product Name Screen Shot][product-screenshot]](https://example.com)

Risk of Rain 2 Mod Manager in JavaFX.

This project is a winter break time-killer turned pet. I wanted to see how I'd fare building a useful application. Take a second to read the install guide and try it out
for yourself!

What it does right now:
* Load and display Mods from Thunderstore API
* Download and extract specific versions of mods. (Currently installs mods directly in game folder).
* Keeps track of installed mods in a SQLite database.
* See which mods you have installed and remove them at your whim.


### Built With

* [Java](https://www.oracle.com/java/)
* [JavaFX](https://openjfx.io/)
* [Maven](https://maven.apache.org/)
* [SQLite](https://www.sqlite.org/index.html)

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- Getting Started -->
## Install Guide

I tried to make this as easy as possible to be cloned and run.

### Prerequisites

This is an example of how to list things you need to use the software and how to install them.
* An IDE
* A SQLite Database Browser. I Recommend [DB Browser for SQLite](https://sqlitebrowser.org/dl/).
  * Important note: You may need the browser for deleting stored mods and starting fresh. Since
  there is not yet a fully implemented feature to update mods, the application can not sustain
  the database seamlessly. Just make sure you write your changes to the database before starting
  the app again or you may run into SQLite write errors.
* [Java SE Development Kit 17.0.1](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) (scroll down a bit).

### Installation

1. Clone the repo
   ```sh
   git clone https://github.com/evanp23/Risk-Of-Rain-2-Mod-Manager
   ```
2. To the project directory (next to ```/src```, ```/DB```, etc.), add two new directories:
    - ```/{Project_Directory}/TempZips```
    - ```/{Project_Directory}/TempExtractions```
3. If not already set, set project SDK to 17 (downloaded above).
4. Set your run configuration to ```controllers.Main```
5. You might want to double check that the ```"directory"``` value in ```Config/Config.json``` is ```""``` (empty string) and that the SQLite Database 
```DB/Config.sqlite``` is empty.

No need to set VM arguments, but because of this, on launch you may see a warning: 

<b>WARNING: Unsupported JavaFX configuration: classes were loaded from 'unnamed module @1362f00d'</b>

It doesn't seem to be causing any actual issues. Tryign to find a better way.

That's it. You should be all up and running. If you find that this information is not getting you to a running application go ahead and make a new issue.

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- ISSUES -->
## Issues and Feature Requests
If you have an issue or would like to request an issue, please [open a new issue][issues-url]

I ask that you please:
1. Keep a submitted issue pertaining to one issue only.
2. Be as descriptive as possible (if you notice a pattern in sequence of events leading up to the issue, describe the sequence of events)
3. If you are able to get it, a screenshot/video of the issue may help.
4. Make note of any errors

If you'd like to work on an issue just request to be put on it and leave some sort of comment about what you might do / have done to fix the issue/fulfill the request.

<!-- CONTRIBUTING -->
## Contributing

All contributions are greatly appreciated.

To contribute, simply:

1. Fork the Project
2. Create your own Branch (`git checkout -b fix/AwesomeFix`)
3. Commit your Changes (`git commit -m 'Kindly fixed {some issue}'`)
4. Push to the Branch (`git push origin fix/AwesomeFix`)
5. Open a Pull Request

Just make sure you reference the issue your pull request is for

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- ROADMAP -->
## Roadmap

- [ ] Complete proper update functionality
- [ ] Complete "Full mod page"
- [ ] Upload manage to Thunderstore
- [ ] Add support for Thunderstore "Download with mod manager" protocol
- [ ] UI Tweaks
- [ ] Get an icon

See the [open issues](https://github.com/evanp23/Risk-Of-Rain-2-Mod-Manager/issues) for a full list of proposed features (and known issues).

<p align="right">(<a href="#top">back to top</a>)</p>


<!-- LICENSE -->
## License

Distributed under the MIT License. See `LICENSE.txt` for more information.

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- CONTACT -->
## Contact

Your Name - [@your_twitter](https://twitter.com/your_username) - email@example.com

Project Link: [https://github.com/your_username/repo_name](https://github.com/your_username/repo_name)

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

Where I got the template for this README:

* [Best-README-Template](https://github.com/othneildrew/Best-README-Template)

<p align="right">(<a href="#top">back to top</a>)</p>



<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/evanp23/Risk-Of-Rain-2-Mod-Manager.svg?style=for-the-badge
[contributors-url]: https://github.com/evanp23/Risk-Of-Rain-2-Mod-Manager/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/evanp23/Risk-Of-Rain-2-Mod-Manager.svg?style=for-the-badge
[forks-url]: https://github.com/evanp23/Risk-Of-Rain-2-Mod-Manager/network/members
[stars-shield]: https://img.shields.io/github/stars/evanp23/Risk-Of-Rain-2-Mod-Manager.svg?style=for-the-badge
[stars-url]: https://github.com/evanp23/Risk-Of-Rain-2-Mod-Manager/stargazers
[issues-shield]: https://img.shields.io/github/issues/evanp23/Risk-Of-Rain-2-Mod-Manager.svg?style=for-the-badge
[issues-url]: https://github.com/evanp23/Risk-Of-Rain-2-Mod-Manager/issues
[license-shield]: https://img.shields.io/github/license/evanp23/Risk-Of-Rain-2-Mod-Manager.svg?style=for-the-badge
[license-url]: https://github.com/evanp23/Risk-Of-Rain-2-Mod-Manager/blob/main/LICENSE.txt
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://www.linkedin.com/in/evan-phillips-73a193164/
[product-screenshot]: images/screenshot.png
