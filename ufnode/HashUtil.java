/** Ben F Rayfield offers this software opensource GNU GPL 2+ */
package ufnode;
import java.util.Arrays;
import util.MathUtil;
import util.Text;
import util.Time;

/** Ufnode (unified merkle forest) is for gaming low lag number crunching in blockchains and sidechains.
Executable binary forest data
structure where every node has a global (100 times faster than sha256) secureHashed int128 name
(TODO fork more secure hash algorithms) of a double, float, 0-8 byte string, 15 bytes of a sha256,
pair of those, etc.

An example global name is uf_d37c$70fd4a944129af73d77d2929b149bf75 which means hash algorithm name (16 bits) d37c
then $ then an int128 whose first byte is type (such as 'g' for tiny string, 'd' for double, 'f' for float, 'F'
for 2 floats, 'h' for the first 7 concat last 8 bytes of a sha256, 'p' for pair of 2 ufnodes etc).

On a 1.6 ghz laptop in Windows 7 I got 10 million hashes per second when allocating an int[4] each time,
and 4 million hashes when reusing the arrays.

The in-progress hash algorithm uses parts of sha256 but is very different as its designed for a loop
of size 16, 128 bit state (other than loop counter), and a constant 256 bit input and 128 bit output.
Knowing its constant sizes allows double hashing (of concat of 2 copies of the input) to prevent
precomputing until nearly the end of the hash and varying just the last part of the input. It therefore
doesnt need the int[64] array of sha256 computed before the main loop. I dont think its a secureHash
yet but is close, and I'll keep improving it, with a different hash algorithm name (like d37c) each.

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
*/
public class HashUtil{
	
	/** 2 uf128s in, 1 uf128 out. All 3 arrays are int[4].
	Strange, but this is faster, when allocating arrays, than reusing arrays directly in hashFast(int[],int[]).
	*/
	public static int[] hash(int[] left, int[] right){
		int[] in = new int[8];
		System.arraycopy(left, 0, in, 0, 4);
		System.arraycopy(right, 0, in, 4, 4);
		int[] out = new int[4];
		hashFast(out,in);
		return out;
	}
	
	/** 32 minorityBit(a,b,c) ops */
	public static int mino(int x, int y, int z){
		return ~((x&y)|(x&z)|(y&z));
	}
	
	/** right rotate aka downshift. Usually gives wrong answer if downshift is negative */
	public static int rr(int data, int downshift){
		return (data<<(32-downshift))|(data>>>downshift);
	}

	/** in is 8 ints (2 uf128s). Writes 1 uf128 to out. */
	public static void hashFast(int[] out, int[] in){
		//first 4 of "first 32 bits of the fractional parts of the square roots of the first 8 primes 2..19" *
		int a = 0x6a09e667;
		int b = 0xbb67ae85;
		int c = 0x3c6ef372;
		int d = 0xa54ff53a;
		for(int doubleHash=0; doubleHash<2; doubleHash++){ //Do the hash on concat(in,in)
			for(int i=0; i<8; i++){
				/*int s1 = (e rightrotate 6) xor (e rightrotate 11) xor (e rightrotate 25);
				int ch = ch := (e and f) xor ((not e) and g);
				int temp1 = h + S1 + ch + k[i] + w[i]
				int S0 = (a rightrotate 2) xor (a rightrotate 13) xor (a rightrotate 22)
				int maj = (a and b) xor (a and c) xor (b and c)
				int temp2 = S0 + maj
				*/
				
				int salt = sha256CuberootSalts[i];
				int inp = in[i];
				int q = (b^salt); //chooser
				int qChAC = (q&~a)|((~q)&c); //each bit in b chooses a bit in the past vs future
				int x = rr(a,6)^rr(a,11)^rr(a,25);
				int y = (rr(b,2)^rr(b,13)^rr(b,22))+inp;
				int z = ((c+salt+inp)^rr(d+qChAC,19));
				int minoXYZ = ~((x&y)|(x&z)|(y&z)); //forest of minorityBit is npcomplete, like forest of nands or nors
				//int next = salt+qChAC+minoXYZ;
				//int next = (qChAC+minoXYZ+inp)^salt;
				int next = qChAC+minoXYZ+inp;
				//System.out.println("next: "+toString(next));
				
				a = b;
				b = c+minoXYZ;
				c = d+salt;
				d = next;
			}
		}
		out[0] = (('p'&0xff)<<24)|(a&0x00ffffff); //replace first byte with type 'p' meaning its a pair (&0xff is a no-op but may help compilers optimize if they first see the 2 masks complement? But maybe I'm just trying to defend it cuz thats how I first wrote it, not considering that all ASCII are positive signed bytes. And this wouldnt even be a bottleneck since its outside the loop.)
		out[1] = b;
		out[2] = c;
		out[3] = d;
	}
	
	/** first 8 of "first 32 bits of the fractional parts of the cube roots of the first 64 primes 2..311"
	TODO verify these are the bits that last sentence describes, once at boot time using BigDecimal or double, else throw,
	and the same for the 4 sha256 squareRoot salts for the starting values of a b c d state in the hashFast func.
	TODO check dotproduct similarity, and audivolv movementScore, to in a basic way verify a near random spread of hashes,
	and other statistical tests, during development of forks of the hash algorithm (each with 16 bit name).
	TODO keep a static final var of the smallest sha256 hashcode known and what input hashes to that,
	among inputs up to 55 bytes (which are 1 sha256 cycle), just to keep track of the security of
	the few standard leaf types (including sha256 and sha3-256) which ufnode allocates core types (max 256 of them) for.
	*/
	private static final int[] sha256CuberootSalts =
		{0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5};
	
	//static int next(int a, int b, int c, int d, int salt, int in){
	//}
	
	public static String toString(int... a){
		String s = "";
		for(int i : a){
			String j = Integer.toBinaryString(i);
			while(j.length() < 32) j = "0"+j;
			s += j;
		}
		return s;
	}
	
	public static String toStringHex(short s){
		return Integer.toHexString(s|0xffff0000).substring(4,8);
	}
	public static String toStringHex(int... a){
		String s = "";
		for(int i : a){
			String j = Integer.toHexString(i);
			while(j.length() < 8) j = "0"+j;
			s += j;
		}
		return s;
	}
	
	/** sha256 is type 'h'. sha3-256 is 'H' */
	public static int[] wrapFirst7AndLast8BytesOfSha256(byte[] b){
		return wrap15BytesWithType((byte)'h',b);
	}
	
	/** sha256 is type 'h'. sha3-256 is 'H' */
	public static int[] wrapFirst7AndLast8BytesOfSha3_256(byte[] b){
		return wrap15BytesWithType((byte)'H',b);
	}
	
	static int[] wrap15BytesWithType(byte type, byte[] b){
		if(b.length != 15) throw new IllegalArgumentException("Wrong size expected 15 bytes: "+b.length);
		return new int[]{
			((type&0xff)<<24)|((b[1]&0xff)<<16)|((b[2]&0xff)<<8)|(b[3]&0xff),
			((b[4]&0xff)<<24)|((b[5]&0xff)<<16)|((b[6]&0xff)<<8)|(b[7]&0xff),
			((b[8]&0xff)<<24)|((b[9]&0xff)<<16)|((b[10]&0xff)<<8)|(b[11]&0xff),
			((b[12]&0xff)<<24)|((b[13]&0xff)<<16)|((b[14]&0xff)<<8)|(b[15]&0xff)
		};
	}
	
	public static int[] wrap(float f){
		return wrap((byte)'f',Float.floatToIntBits(f)&0xffffffffL);
	}
	
	public static int[] wrap(float left, float right){
		return wrap((byte)'F',(((long)Float.floatToIntBits(left))<<32)|(Float.floatToIntBits(right)&0xffffffffL));
	}
	
	public static int[] wrap(double d){
		return wrap((byte)'d',Double.doubleToLongBits(d));
	}
	
	public static int[] wrap(long j){
		return wrap((byte)'j',j);
	}
	
	public static int[] wrap(int i){
		return wrap((byte)'i',i&0xffffffffL);
	}
	
	public static String toUri(int[] uf){
		return toUri(hashAlgorithmName, uf);
	}
	
	/** A kind of URI (which URL and URN are subtypes of), the global name of the ufnode */
	public static String toUri(short hashAlgorithmName, int[] uf){
		return "uf_"+toStringHex(hashAlgorithmName)+"$"+toStringHex(uf);
	}
	
	/** type 'g' (tiny strinG).
	0-8 utf8 bytes (using 2 0 bytes for codepoint 0, allowing 2*3 bytes or 4 bytes for high codepoints)
	FIXME do one of:
	-- put 4 bits of size (0-8) (reducing hash part from 56 to 52 bits),
	-- pad prefix or pad suffix with byte0s which practically arent used in strings,
	        but problem will occur in bytestring 0-8 which will also be supported.
	-- Use 9 type bytes for utf8String and 9 type bytes for byteString (such as chars '0'-'9').
	*/
	public static int[] wrap(String s){
		if(s.length() > 8) throw new Error("Too long: "+s);
		byte[] utf8 = Text.stringToBytes(s);
		if(utf8.length > 8) throw new Error("Too long ("+utf8.length+" utf8 bytes): "+s);
		if(utf8.length < 8){
			byte[] prefixPadded = new byte[8];
			Arrays.fill(prefixPadded, (byte)'_');
			System.arraycopy(utf8, 0, prefixPadded, 8-utf8.length, utf8.length);
			utf8 = prefixPadded;
		}
		return wrap((byte)'g',MathUtil.bytesToLong(utf8));
	}
	
	public static Object unwrap(int[] uf128){
		throw new Error("TODO");
	}
	
	public static String unwrapString(int[] uf128){
		throw new Error("TODO");
	}
	
	public static double unwrapDouble(int[] uf128){
		throw new Error("TODO");
	}
	
	public static float unwrapFloat(int[] uf128){
		throw new Error("TODO");
	}
	
	public static float unwrapLeftFloat(int[] uf128){
		throw new Error("TODO");
	}
	
	public static float unwrapRightFloat(int[] uf128){
		throw new Error("TODO");
	}
	
	public static int[] wrap(byte... zeroTo8Bytes){
		throw new Error("TODO");
	}
	
	/** returns an uf128. 9 bytes are literal data, including 1 type byte. 7 bytes are a hash of that.
	Those 7 bytes will be a different kind of hash than the pair hasher of 256 to 128 bits.
	Its a hasher of long to long (ignoring type byte), and 1 byte of that output long is ignored.
	*/
	public static int[] wrap(byte type, long literal){
		long hash = hashLiteral(literal);
		long prefix = ((type&0xffL)<<56)|(hash&0x00ffffffffffffffL);
		return new int[]{ (int)(prefix>>32), (int)prefix, (int)(literal>>32), (int)literal };
	}
	
	public static long hashLiteral(long literal){
		throw new Error("TODO");
	}
	
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
	
	public static void main(String[] args){
		System.nanoTime(); //dont count booting the clock in the stats
		int[] left = new int[4];
		int[] right = new int[4];
		System.out.println(toString(hash(left,right)));
		right[3] = 1;
		System.out.println(toString(hash(left,right)));
		right[3] = 2;
		System.out.println(toString(hash(left,right)));
		right[3] = 3;
		System.out.println(toString(hash(left,right)));
		for(int i=0; i<100; i++){
			left[2] = i;
			int[] h = hash(left,right);
			//System.out.println("uf$"+toStringHex(h)+" "+toString(h));
			System.out.println(toUri(h)+" "+toString(h));
		}
		
		//speed test
		int[] in256 = new int[8];
		int[] out = new int[4];
		long start = System.nanoTime();
		int cycles = 1000000;
		for(int i=0; i<cycles; i++){
			for(int j=0; j<8; j++){
				in256[j] = i+j;
			}
			hashFast(out, in256);
		}
		long end = System.nanoTime();
		double seconds = (end-start)*1e-9;
		double hz = cycles/seconds;
		System.out.println("hashes per second: "+hz);
		
		start = System.nanoTime();
		left = new int[4];
		right = new int[4];
		for(int i=0; i<cycles; i++){
			for(int j=0; j<4; j++){
				left[j] = i+j;
				right[j] = j-i;
			}
			hash(left, right);
		}
		end = System.nanoTime();
		seconds = (end-start)*1e-9;
		hz = cycles/seconds;
		System.out.println("hashes per second when alloc arrays (strange, why is this faster?): "+hz);
		
		/*
		2336eb873c9bc65bbe6faa5cb47a455e 00100011001101101110101110000111001111001001101111000110010110111011111001101111101010100101110010110100011110100100010101011110
		2e8cb850f61294599f67b8e97a11d2db 00101110100011001011100001010000111101100001001010010100010110011001111101100111101110001110100101111010000100011101001011011011
		90db73c064663d8da9b7b49c56275484 10010000110110110111001111000000011001000110011000111101100011011010100110110111101101001001110001010110001001110101010010000100
		46f2eef332bea3d91a956d869964a3e0 01000110111100101110111011110011001100101011111010100011110110010001101010010101011011011000011010011001011001001010001111100000
		7c0ed34b9a7dac5c493d182ed67c2fe9 01111100000011101101001101001011100110100111110110101100010111000100100100111101000110000010111011010110011111000010111111101001
		hashes per second: 4159935.2501086392
		*/
	}

}
