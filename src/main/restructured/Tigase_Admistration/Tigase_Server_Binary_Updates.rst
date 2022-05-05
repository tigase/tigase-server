Tigase Server Binary Updates
==================================

Most open source projects try to make sure that the nightly builds compile correctly so that these builds can be used. However, we at Tigase believe that these builds should be separated until they are thoroughly tested and released. Although lots of installations out there we know of just run from our nightly builds, this puts an extra responsibility to make sure all code is functional and will constantly work. Therefore, our general approach is to run all basic functionality tests before each code commit to make sure it works correctly. This does not guarantee that there will never be a problem, but it is a precaution from preventing bad builds from arriving in the hands of our customers.

Some users on the other hand, like to be on the bleeding edge and regularly use our nightly builds exploring new code changes and playing with new features before they are put to a full release. Others prefer to stick to stable and fully tested public releases. Others however, want something from the middle, the most recent features, but bug fixes, something like a beta or a release-candidate state.

Should you choose to use the nightly builds, a few things you should consider:

-  Changes may be made to the code that can negatively affect performance.

-  Changes may be made to the code that can negatively affect security.

We **highly** recommend testing these builds in your environments before upgrading.

With these considerations in mind, we provide nightly builds at `this link <https://build.tigase.net/nightlies/dists/>`__ which provides directories by date.

Standard naming format is ``tigase-server-<version>-SNAPSHOT-b<build>-<type>`` where ``<version>`` is in the form of ``major.minor.bugfix``

.. Note::

   individual days may have the same builds as noted by the byyyy section of the file.\*

Just like the standard distributions, the builds are available with the following extensions (``<type>``):

1. ``javadoc.jar`` - Java installer for javadoc only

2. ``dist.zip`` - Compressed binaries with no dependencies.

3. ``dist.tar.gz`` - tarball compressed binaries with no dependencies.

4. ``dist-max.zip`` - Compressed binaries with all dependencies.

5. ``dist-max.tar.gz`` - tarball compressed binaries with all dependencies.

We also provide automated testing of each of our nightly builds for each supported databases. Tests are done with both functional and low memory parameters in mind, and are available `at this link <https://build.tigase.net/nightlies/tests/>`__. These tests can provide a quick examination of function before upgrading your current build.
