Welcome to Play framework
=========================

This is a fork of play! framework (http://playframework.org/).

---------------
Getting started
---------------

1. Install the latest version of Play framework and unzip it anywhere you want::

     unzip play-1.2.zip -d /opt/play-1.2

2. Add the **play** script to your PATH::

     export PATH=$PATH:/opt/play-1.2

3. Create a new Play application::

     play new /opt/myFirstApp

4. Run the created application::

     play run /opt/myFirstApp

5. Go to "localhost:9000/":http://localhost:9000 and you’ll see the welcome page.

6. Start developing your new application:

   * "Your first application — the ‘Hello World’ tutorial":http://www.playframework.org/documentation/1.1/firstapp
   * "Tutorial — Play guide, a real world app step-by-step":http://www.playframework.org/documentation/1.1/guide1
   * "The essential documentation":http://www.playframework.org/documentation/1.0.3/main
   * "Java API":http://www.playframework.org/@api/index.html


--------------
Get the source
--------------

Fork the project source code on `Github`_::

  git clone git://github.com/moriyoshi/play-forked.git

The project history is pretty big. You can pull only a shallow clone by specifying the number of commits you want with **--depth**::

  git clone git://github.com/moriyoshi/play-forked.git --depth 10

.. _GitHub: http://github.com/moriyoshi/play-forked

--------------
Reporting bugs
--------------

Please report bugs on `GitHub's issue tracker`_.

.. _GitHub's issue tracker: _http://github.com/moriyoshi/play-forked/issues

-------
License
-------

Play framework is distributed under `Apache 2 license`_.

.. _Apache 2 license: http://www.apache.org/licenses/LICENSE-2.0.html
