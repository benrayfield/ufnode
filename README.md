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
		
https://en.wikipedia.org/wiki/Hash_consing
TODO implement ufmaplist (with optional homomorphic dedup requiring secret salt per computer,
for global dedup any pair of computers which dont trust eachother will agree on)...
Implement the ufmaplist datastruct (see code in the ufnodeOld and ufnodeapi javapackages
in (still disorganized to be published soon hopefully) at the binary forest level
so an ufmaplist is made of multiple binary forest nodes such as a linkedlist of its fields
including min key, max key, long list size, ufnode.ty byte, etc,
and different ufnode.ty (small constant set of ufnode types from which complex things are built)
are different datastructs. An ufmaplist is an avl treemap or treelist, like in benrayfields wavetree software.
TODO create a binary forest (instead of var size maplist) based variation of ufnode.Security class's opcodes
where every func is called like [? "someNameOfTheFuncThatDescribesItVeryPrecisely]#prehopfieldname
and prehopfieldname can after that in the same code string refer to that node in the immutable forest
and will not differ in computing behavior when multiple people locally name the same uf128 different things.
TODO lazyEvalHashing for opencl optimized matrixMultiply of RBM neuralnet (as in the paint* rbm experimental code
I've been developing which is still too disorganized), which could hook in through sha256 of such arrays
before and after opencl ops whose inputs and outputs are much smaller than the number of calculations done,
or could reflect pairs of floats or single doubles each as an uf128. Ufnode in those cases
would not be the bottleneck but is close to it so be careful to keep it fast,
or more practically lazyEvalHashing would avoid hashing within local computer until
save to harddrive or send across Internet (any untrusted border) need to compute uf128 recursively
and in the process the benefit of dedup.
Pure determinism vs allowing roundoff (such as opencl claims strictfp option but its said to work on some computers
but not others), order of ops, etc... By default things are slightly nondeterministic
but there should be a puredeterministic mode or some types guarantee it per node.
TODO linkedhashtable with 256 bit buckets (or 384 if store what they hash to)
andOr various other optimized datastructs for lookup and storing and sharing uf128s
and bitstrings which 'h' and 'H' (external hash algorithms sha*) point at.
Buckets might be int32s pointing into an array used as a heap/hashtablecontents.
For faster computing without dedup/sharing (yet or garbcol before that so never for that node),
an object in memory using native pointers (java pointers, javascript pointers, C pointers, etc) instead of 128 bit ids
use them until trigger lazyEvalHash.
TODO use econacyc of com and mem and zapeconacyc, optionally, depending what kinds of sandboxing is needed,
such as guaranteeing escape from a function called within a microsecond of it running out of allocated
compute cycles and memory allocation, or statistical sharing of memory cost upward along reverse pointers,
with a high cost of such zapeconacyc statistics. Allows things like running a virus in debug mode
purely statelessly without risk if the ufnode VM correctly sandboxes, similar to how a web browser
can safely go to websites without those websites being able to run programs on your computer
modifying your private files etc. Javascript's security holes seem to be in the native objects
outside the javascript memory space such as Flash and DRM video players.
Ufnode has no such state. All nodes are immutable merkle forest.
For example, you cant command it to write to stdOut or read from the keyboard.
There is no input or output, only the fact of a binary forest with certain types of leafs allowed.
A virus may only execute its next computing step to return another virus but may not modify anything,
and if you choose to use the new virus it returns, similarly it cant modify itself, only return yet another virus,
so there is no such thing as a virus, meaning that which statefully modifies things without permission,
since nothing ever modifies anything.
An ufnode is a kind of number. It always halts within some guaranteed max time and memory, but its not always turingComplete.
It is turingComplete if thats run in a loop, but each execution of the loop body always halts, like a debugger.
