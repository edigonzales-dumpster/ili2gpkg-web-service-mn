#!/bin/sh
docker build . -t ili2gpkg
echo
echo
echo "To run the docker container execute:"
echo "    $ docker run -p 8080:8080 ili2gpkg"
