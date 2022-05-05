Cluster Map Interface
==========================

Starting with v7.1.0, a cluster map interface has been implemented. The cluster map is aided by use of the distributed event bus system to communicate between all clusters.

Requirements
--------------------------

Any full distribution of Tigase will support the Cluster Map API so long as the eventbus component is not disabled. JDK v8 is required for this feature, however since Tigase requires this, you should already have it installed.

The cluster map is stored in memory and follows the ``map.util.interface`` java standards can be used to improve cluster connections, and help clustered servers keep track of each other.

Map Creation
----------------

Map must be created with the following command:

.. code:: java

   java.util.Map<String, String> map = ClusterMapFactory.get().createMap("type",String.class,String.class,"1","2","3" )

Where "type" is the map ID. This creates the map locally and then fires an event to all clustered servers. Each cluster server has an event handler waiting for, in this case, ``NewMapCreate`` event. Map Key class and Map Value class are used to type conversion. Arrays of strings are parameters, for example ID of user session. Once received, the distributed eventbus will create a local map.

.. code:: java

   eventBus.addHandler(MapCreatedEvent.class, new EventHandler<MapCreatedEvent>() {
       @Override
       public void onEvent(MapCreatedEvent e) {
       }
   });

A brief example of a map creation is shown here:

.. code:: java

   java.util.Map<String, String> map = ClusterMapFactory.get().createMap("Very_Important_Map_In_User_Session",JID.class,Boolean.class,"user-session-identifier-123");

This will fire event ``MapCreatedEvent`` on all other cluster nodes. Strings "Very_Important_Map_In_User_Session" and "user-session-identifier-123" are given as parameters in :literal:`onMapCreated()\`` method. The event consumer code must know what to do with map with type "Very_Important_Map_In_User_Session". It may retrieve user session "user-session-identifier-123" and put this map in this session. It should be used to tell other nodes how to treat the event with a newly created map, and it should be stored in user session.

Map Changes
----------------

Changes to the map on one cluster will trigger ``AddValue`` or ``RemoveValue`` events in eventbus. Stanzas sent between clusters will look something like this:

.. code:: xml

   <ElementAdd xmlns="tigase:clustered:map">
     <uid>1-2-3</uid>
     <item>
       <key>xKEY</key>
       <value>xVALUE</value>
     </item>
     <item>
       <key>yKEY</key>
       <value>yVALUE</value>
     </item>
   </ElementAdd>

Code to handle adding an item:

.. code:: java

   eventBus.addHandler(ElementAdd, tigase:clustered:map, new EventHandler() {
     @Override
     public void onEvent(String name, String xmlns, Element event) {
     });

Where the element 'event' is the UID, and the name string is the name of the map key/value pair.

This example removes an element from the cluster map. Removal of items look similar:

.. code:: xml

   <ElementRemove xmlns="tigase:clustered:map">
     <uid>1-2-3</uid>
     <item>
       <key>xKEY</key>
       <value>xVALUE</value>
     </item>
   </ElementRemove>

with the code also being similar:

.. code:: java

   eventBus.addHandler(ElementRemove, tigase:clustered:map, new EventHandler() {
     @Override
     public void onEvent(String name, String xmlns, Element name) {
     });


Map Destruction
------------------

Java Garbage Collector will normally remove a local map if it is no longer used. Clustered maps however are not removed in this manner. These maps must be destroyed manually if they are no longer used:

.. code:: java

   ClusterMapFactory.get().destroyMap(clmap);

Calling this, the map named clmap will be destroyed on each cluster node.

The event handler will catch event when map is destroyed on another cluster node:

.. code:: java

   eventBus.addHandler(MapDestroyedEvent.class, new EventHandler<MapDestroyedEvent>() {
       @Override
       public void onEvent(MapDestroyedEvent event) {
       }
   });
