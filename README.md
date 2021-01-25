# ProfileDeleter

Deletes local profile folders and registry keys from a remote computer.

Should work on Windows 7 and higher versions.

# Work needed

To do:

* Try and make tooltips redraw if moving between two cells that show the same tooltip. Currently if two adjacent cells show the same tooltip the tip won't redraw. This is unintuitive and just looks like program lag.
* Add GUI popup that alerts user if state check and registry backup processes have failed most likely due to not enough disk space to load the users profile on the remote machine or to save the .reg backup files temporarily on the remote machine, possibly also give option to automatically delete some files on the remote computer from temp locations.
* Make it rename the users folder first, then delete the registry keys, then delete the renamed user folder. This will ensure the user has no trouble logging in if something goes wrong or if the program is ended while a deletion is running.

Bugs:

