# PostProcessor

This project lets you save RSL routines from Visual Components, and convert them into source code for the KUKA robots.
It does a source to source translation, with a couple caveats.

When using "Define tool centerpoint" statements you should define the tool relative to the flange on the robot.
Otherwise there will be issues with your tool frame being in a wierd location.
If you want to navigate in relation to a tool centerpoint you should define this with a "Define tool statement".
There is not enough information saved by Visual Components if you just select an existing tool, and don't define a TCP.
The only way to get hold of this information is to hack this up manually with a "Define tool statement".

Likewise there's a similar situation with defining base frames. They should be defined in the world coordinate system, and not relative.
There is no support for relative motions yet, this is something that could be added at a later time.
Also, there is no support for multiple base frames, the main base frame of the robot is moved each time.
This means that you will need to move the base frame right before you're supposed to be using it.
Since the base frames are defined in the world coordinates, you will need to make sure there is a correlation between your simulation and your robot's understanding of world frame.
The simplest is to position your robot's base to coincide with the world frame, as that is how the robots are delived as default.
If that is not suitable, support could be added for a "world-frame offset" or one can adjust the $ROBROOT variable in R1\MADA\$MACHINE.DAT.

The translator also has subroutine support, allowing you to abstract your robot programming.
