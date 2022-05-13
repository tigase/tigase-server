
EventBus API in Tigase
============================

EventBus is a custom publish-subscribe mechanism which allows for the use of Event Listener within Tigase Server. For a more detailed overview of EventBus and it’s features, please visit `The Administration Guide <http://docs.tigase.org/tigase-server/snapshot/Administration_Guide/html/#eventBus>`__.

EventBus API
-----------------

To create instance of EventBus use the following code:

.. code:: java

   EventBus eventBus = EventBusFactory.getInstance();

.. Note::

   Remember, that EventBus is asynchronous. All handlers are called in a different thread than the thread that initially fired the event.

Events
^^^^^^^^^^^

Events may be defined in two ways: as a class |ss| or as an XML element(XML/Element based events are deprecated since version 8.2 and will be removed in version 9.0)\. |se|\

**Serialized event class.**

.. code:: java

   public class SampleSerializedEvent implements Serializable {
       private JID data;
       public JID getData() {
           return this.data;
       }
       public void setData(JID data) {
           this.data = data;
       }
   }

**Event class.**

.. code:: java

   public class SampleEvent {
       private JID data;
       public JID getData() {
           return this.data;
       }
       public void setData(JID data) {
           this.data = data;
       }
   }

|ss| **XML Element event(deprecated)**\ |se|\

.. code:: xml

   <EventName xmlns="tigase:demo">
     <sample_value>1</sample_value>
   </EventName>

.. Note::

   Events defined as XML element and class implementing ``Serializable`` interface will be distributed to all servers in cluster. Event ``SampleEvent`` will be broadcast only in the same instance what fired the event.

Requirements for class-based events
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

-  Default, explicit, public, paremeter-less constructor is mandatory.

-  If the event should be delivered to all cluster nodes then it **MUST** implement ``Serializable`` interface.

-  Variables serialisation follows ``Serializable`` semantics, which means that ``final``, ``static`` nor ``transient`` fields will be skipped. What’s more, fields with ``null`` value will not be serialised neither.

Serialisation of class-based events
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Class based events are serialized (if it is required and possible) to XML element. Name of XML element is taken from full name of class:

**Class based event serialized to XML.**

.. code:: xml

   <net.tigase.sample.SampleSerializedEvent>
       <data>sample@data.tigase.net</data>
   </net.tigase.sample.SampleSerializedEvent>

Firing events
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To fire event, just get instance of EventBus and call method ``fire()``.

**Firing serialized event.**

.. code:: java

   EventBus eventBus = EventBusFactory.getInstance();
   SampleSerializedEvent event = new SampleSerializedEvent();
   eventBus.fire(event)

**Firing simple event.**

.. code:: java

   EventBus eventBus = EventBusFactory.getInstance();
   SampleEvent event = new SampleEvent();
   eventBus.fire(event)

|ss| **Firing event based on XML Element(deprecated)** |se|\

.. code:: java

   EventBus eventBus = EventBusFactory.getInstance();
   Element event = new Element("tigase.eventbus.impl.Event1");
   eventBus.fire(event)

Handling events
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To handle fired event, we have to register listener in EventBus. When listener is registered, EventBus automatically subscribes for this type of event in all instances in cluster.

Depends on expected event type, we have to decide what type of listener we should register.

Handling class based events
~~~~~~~~~~~~~~~~~~~~~~~~~~~

This option is reserved for class based events only. It doesn’t matter if it is serialized class or not.

.. code:: java

   eventBus.addListener(SampleEvent.class, new EventListener<SampleEvent>() {

       @Override
       public void onEvent(SampleEvent event) {
       }
   });

To make registering listeners more easy, you can use method ``registerAll()`` from EventBus. This method registers all methods given class, annotated by ``@HandleEvent`` as listeners for event declared as the method argument.

.. code:: java

   public class SomeConsumer {

       @HandleEvent
       public void event1(Event12 e) {
       }

       public void initialize() {
           eventBus.registerAll(this);
       }
   }


|ss| Handling XML events |se|\
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To handle XML events we have to register listener for specific event package and name. In our example, package is empty because event name has no package declared (see also `Filtering events <#_filtering_events>`__).

.. code:: java

   eventBus.addListener("", "EventName", new EventListener<Element>() {
       @Override
       public void onEvent(Element event) {

       }
   });
   eventBus.addListener("tigase.eventbus.impl", "Event1", new EventListener<Element>() {
       @Override
       public void onEvent(Element event) {

       }
   });

Because serialized class events, ale transformed to XML elements, we are able to listen for XML representation of class based event. To do that, we have to register listener for specific package and class name:

.. code:: java

   eventBus.addListener("net.tigase.sample", "SampleSerializedEvent", new EventListener<Element>() {
       @Override
       public void onEvent(Element event) {

       }
   });

..

   **Important**

   XML events created on others cluster node, will have attribute ``remote`` set to ``true`` and attribute ``source`` set to event creator node name:

   .. code:: xml

      <EventName xmlns="tigase:demo" remote="true" source="node1.example">
        <sample_value>1</sample_value>
      </EventName>


Filtering events
~~~~~~~~~~~~~~~~

Sometimes you may want to receive many kinds of events with the same handler. EventBus has very simple mechanism to generalization:

.. code:: java

   eventBus.addListener("net.tigase.sample", null,  event -> {}); 
   eventBus.addListener(null, null,  event -> {}); 

-  This listener will be called for each event with given package name (XML based, or serialized class based).

-  This listener will be called for ALL events (XML based, or serialized class based).

In case of class based events, EventBus is checking class inheritance.

.. code:: java

   class MainEvent { }
   class SpecificEvent extends MainEvent {}

   eventBus.addListener(SpecificEvent.class, event -> {}); 
   eventBus.addListener(MainEvent.class, event -> {}); 

   eventBus.fire(new SpecificEvent());

-  Will be called, because this is listener stricte for ``SpecificEvent``.

-  Will be called, because ``SpecificEvent`` extends ``MainEvent``.


.. |ss| raw:: html

    <strike>
.. |se| raw:: html

    </strike>