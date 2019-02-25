# ProfileDeleter

Deletes local profile folders and registry keys from a remote computer.

Should work on Windows 7 and higher versions.

# Work needed

To do:

* Try and make tooltips redraw if moving between two cells that show the same tooltip. Currently if two adjacent cells show the same tooltip the tip won't redraw. This is unintuitive and just looks like program lag.
* Try and make registry functions also create a .reg backup file with REG EXPORT so ProfileList and ProfileGuid can be easily recovered in the case of a major error.
* Remove the need to save REG QUERY results to file.

Bugs:

