# ufnode
For gaming low lag number crunching in blockchains and sidechains. Executable binary forest data structure where every node has a global (100 times faster than sha256) secureHashed int128 name (TODO fork more secure hash algorithms) of a double, float, 0-8 byte string, 15 bytes of a sha256, pair of those, etc.

An example global name is uf_d37c$70fd4a944129af73d77d2929b149bf75 which means hash algorithm name (16 bits) d37c then $ then an int128 whose first byte is type (such as 'g' for tiny string, 'd' for double, 'f' for float, 'F' for 2 floats, 'h' for the first 7 concat last 8 bytes of a sha256, 'p' for pair of 2 ufnodes etc).

On a 1.6 ghz laptop in Windows 7 I got 10 million hashes per second when allocating an int[4] each time, and 4 million hashes when reusing the arrays.

The in-progress hash algorithm uses parts of sha256 but is very different as its designed for a loop of size 16, 128 bit state (other than loop counter), and a constant 256 bit input and 128 bit output. Knowing its constant sizes allows double hashing (of concat of 2 copies of the input) to prevent precomputing until nearly the end of the hash and varying just the last part of the input. It therefore doesnt need the int[64] array of sha256 computed before the main loop. I dont think its a secureHash yet but is close, and I'll keep improving it, with a different hash algorithm name (like d37c) each.

/** Each version of ufnode's hash algorithm is a 16 bit hashcode, whatever are the first 16 bits of
	hash(pair("pi is",(double)3.14159265358979323846)) hashes to, for example, in text write it as
	uf_d79b$70fcddd49b05528f5a58bc919ae55191. This allows many opensource forks of ufnode to work
	together like urls to different networks, given the cooperation of people to not use 2 forks of
	ufnode intentionally designed to collide on those 16 bits. The 120 bit hashcode (and 8 bit type)
	is either a secureHash already or will be pursued in such hash variations until find an efficient
	securehash of pair(128bits,128bits) to 128bits, efficient like 10 million hashes per second on an
	average computer.
	*/
	public static final short hashAlgorithmName = (short)0xd37c;
		//FIXME TODO (short)(hash(wrap("pi is"),wrap(java.lang.Math.PI))[0]>>>16);