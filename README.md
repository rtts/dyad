# Contents

  * Introduction
  * Dyad Server
    * Overview
    * Installation
    * Perlpod
  * Dyad Client
    * Installing the Dyad Client Android Library
      * Choose an Android target
      * Linking to the Android Support Library
      * Installing Bump
    * Examples
    * Javadoc
  * Frequently Asked Questions

# Introduction

In today's world of
[NAT](http://en.wikipedia.org/wiki/Network_address_translation)s and
firewalls, it's a problem for anyone but Skype and your phone company to
establish a direct communication socket between two mobile devices. The Dyad
Project solves this problem by combining
[GCM](http://developer.android.com/guide/google/gcm/index.html) and
[ICE](http://tools.ietf.org/html/rfc5245) to create streaming peer-to-peer
connections between apps running on Android devices.

It roughly works like this:

The Dyad Server keeps a database of pairs of GCM ids, called **Dyads**. The
Dyad Client is an Android Library compiled into an app. When the Dyad Client
contacts the Dyad Server, the server fires a GCM message to the correct
device, which wakes up and also contacts the server. Through the server, both
apps retrieve the information needed to connect directly through ICE. You can
configure Dyad to fall back to a relayed
([TURN](http://en.wikipedia.org/wiki/TURN)) connection in case this fails.
Both apps now possess a UDP communication socket that they can use to send
streaming data directly to the other device.

The Dyad Project is completely open source and app developers can use it
freely. Here's what you need to do to incorporate peer-to-peer-functionality
into your own app:

  1. Get the [source code](http://github.com/r2src/dyad/). 

  2. Install Dyad Server on a publicly accessible server somewhere. 

  3. Configure the Dyad Client Library and compile it into your app. 

# Dyad Server

## Overview

A new User first registers itself with the server. If this succeeds a session token is
returned. If the User wishes to form a Dyad with another User, they
decide on a shared secret and send this to the server. Both are
notified when the Dyad is formed. When a User wants to create a Stream, the server
is used as a relay to send SDP messages.

## Installation

The source code includes a makefile. Compile it by running `perl Makefile.pl`.

## Perlpod

# Dyad Client

The Dyad Client is organized as an [Android Library Project](http://developer.
android.com/tools/projects/index.html#LibraryProjects) that you can import
into your own projects.

## Installing the Dyad Client Android Library

The Dyad Client Android Library is located in the source distribution in the
subdirectoy `client`. To be able to use it, you first need to install the
Android SDK from
[http://developer.android.com/sdk/](http://developer.android.com/sdk/). After
that, you have to configure the Dyad Client Android Library by updating it
with an Android target and, optionally, with the [Bump
SDK](http://bu.mp/company/api) if you want to use Bump to perfom the bonding
of dyads.

### Choose an Android target

First, run the command `android list targets` and choose the number that
belongs the Android target you are developing your app for. You can install
new targets by running just `android`. The minimum Android version supported
by Dyad is Android 2.2 (API 8).

Second, run the command `android update lib-project --target {target} --path
{dir}/dyad/client` where {target} is the number you found in the previous step
and {dir} is the path to the directory where you placed the Dyad source code.

### Linking to the Android Support Library

If your app targets an Android version below 3.0, the Dyad Client Library
needs the [Android support library](http://developer.android.com/tools/extras
/support-library.html). Copy [android-
support-v4.jar](http://developer.android.com/tools/extras/support-
library.html) from the directory `android-sdk/extras/android/support/v4/` to
`dyad/client/libs/`. If you're developing in Eclipse, you can also right-click
your project and select `Android Tools > Add Support Library`.

### Installing Bump

If you want to use Bump for bonding, [sign up as a
developer](http://bu.mp/company/api) for Bump and install the Bump SDK. Then,
copy the file BumpBonder.java from the directory `dyad/client/extras` to
`dyad/client/src/com/r2src/dyad`. You can then use the Bump Bonder like any
other bonder.

## Configuring Eclipse

Start new Android Project from existing source, use /dyad/client as the
project directory. (the previous steps already configured this project as an
Android Library Project) TODO: provide simple project.properties for both
projects. Start new Android Project from existing source, use
/dyad/client/test as the project directory. Don't start an Android Test
Project, because a Test Project cannot test an Android Library Project. Start
new Perl Project. Use dyad/server as the source directory.

## Examples

## Javadoc

# Frequently Asked Questions
