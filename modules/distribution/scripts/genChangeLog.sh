#!/bin/bash

git log --pretty --numstat --summary tigase-server-5.1.0..master | scripts/git2cl > ChangeLog
