# Introduction #

GanttProject Cloud is our cloud service for file sharing which is compatible with WebDAV protocol. At the moment it is in trusted testing and special sign-up request is required from you.

# Sign in to GanttProject Cloud #

You need to make sure that you can sign-in to GanttProject Cloud on https://cloud.ganttproject.biz After successful sign-in you will see a list of projects you have access to. Normally we share a sample project when we process your sign-up request, so the list should not be empty.

On the right side you see settings for configuring your desktop GanttProject.

# GanttProject set up #
Start GanttProject, open WebDAV settings page (_Edit->Settings->WebDAV_), click Add, create a descriptive name for the server (e.g. GP) and copy settings from your GP Cloud page into appropriate fields. Check _"save password"_ field to save your password (note that it is saved in the settings file and thus becomes visible to everyone who have read access to that file). You're done.

# Opening files from GP Cloud #
Start GanttProject, then go to _Project->Web Server->Open from a server_, make sure that GP server is selected. You should see a list of projects you have access to. Select one and click "Open"

# Saving files to GP Cloud #
If you opened a file from GP Cloud, it will be saved back to GP Cloud when you click Save. If you opened a file from your local disk, you can save it to GP Cloud with _Project->Web Server->Save to a server_. Don't forget to give a name to your file.

# Sharing files with your colleagues #
Currently you can do it from the web interface only.

Let "host" be the person who want to share a project and "guest" the one with whom host wants to share. How the process looks like from hosts's perspective:

## Host ##
Sign in to GP Cloud, open the list of projects and click the project you want to share. You'll see a sharing list. Type email of the person you want to share with, select access permissions (reader/writer/owner). If you know that your guest is already signed up, please use the email which he used when signing up. If your guest is not yet signed up, you can use any his email. In this case check "send emails" checkbox, as your guest will receive an invitation code by email.

## Guest ##
If guest is already signed up, and host used an email associated with the guest, he will be able to access shared file immediately.

If guest is not signed up, and host added his email to the sharing list and checked "send email" option, the guest will receive an email with the invitation code. He needs to pass through sign-up process, enter invitation code when asked and will be able to access shared file after that. In this case guest does't need to wait for our approval.

## Working on the same project ##
Guest and host each use their own credentials (email and password) to access GP Cloud.

Guest will be able to read or read/write the shared file depending on the permissions set by host (read/write/owner).

If both host and guest have write permissions they both will be able to overwrite a file (GP makes no additional checks) unless one of them holds a lock. Lock prevents anyone except for the lock owner from writing. To acquire a lock, select a file in the list in "Open from web server" dialog in GanttProject and click padlock button. Make sure that "lock timeout" value in WebDAV settings is set to positive value.