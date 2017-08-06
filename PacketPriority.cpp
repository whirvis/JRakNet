/// These enumerations are used to describe when packets are delivered.
enum PacketPriority
{
	IMMEDIATE_PRIORITY, /// The highest possible priority. These message trigger sends immediately, and are generally not buffered or aggregated into a single datagram.
	//Messages at HIGH_PRIORITY priority and lower are buffered to be sent in groups at 10 millisecond intervals
	HIGH_PRIORITY,   /// For every 2 IMMEDIATE_PRIORITY messages, 1 HIGH_PRIORITY will be sent.
	MEDIUM_PRIORITY,   /// For every 2 HIGH_PRIORITY messages, 1 MEDIUM_PRIORITY will be sent.
	LOW_PRIORITY,   /// For every 2 MEDIUM_PRIORITY messages, 1 LOW_PRIORITY will be sent.
	NUMBER_OF_PRIORITIES
};