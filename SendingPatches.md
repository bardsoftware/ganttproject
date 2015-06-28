# How to send your patch #

Okay, so you hacked a new feature or fixed a bug and now want to integrate your changes into GanttProject trunk. There are two ways of doing that.

## Send a patch by email ##
This will work if your patch is small (say, about 10 lines). Prepare your patch:
  * In Eclipse, context menu on the project root, then _Team->Create Patch..._

and just send it to contribution@ganttproject.biz

## Create a clone project and request a code review ##
If you patch is big enough, we kindly ask you to pass a code review. To make reviewing easier, create a server-side clone of GP repository and enable non-members to review its code in the _Source_ administration settings. Then submit your changes to the clone and [file a code review issue](http://code.google.com/p/ganttproject/issues/entry?template=Review%20request).