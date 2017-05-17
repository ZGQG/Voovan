package org.voovan.tools;

import org.voovan.tools.Exception.MemoryReleasedException;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import sun.misc.Cleaner;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ByteBuffer双向通道
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ByteBufferChannel {

	private long address;
	private Unsafe unsafe = TUnsafe.getUnsafe();
	private ByteBuffer byteBuffer;
	private int size;
	private ReentrantLock lock ;

	private Deallocator deallocator;
	private Cleaner cleaner;

	/**
	 * 构造函数
	 * @param capacity 分配的容量
	 */
	public ByteBufferChannel(int capacity) {
		init(capacity);
	}

	/**
	 * 构造函数
	 */
	public ByteBufferChannel() {
		init(256);
	}

	/**
	 * 初始化函数
	 * @param capacity 分配的容量
	 */
	private void init(int capacity){
		lock = new ReentrantLock();
		this.byteBuffer = newByteBuffer(capacity);
		byteBuffer.limit(0);
		resetAddress();
		this.size = 0;
	}

	/**
	 * 构造一个ByteBuffer
	 * @param capacity 分配的容量
	 * @return ByteBuffer 对象
	 */
	private ByteBuffer newByteBuffer(int capacity){
		try {
			ByteBuffer template = ByteBuffer.allocateDirect(0);
			Constructor c = template.getClass().getDeclaredConstructor(long.class, int.class);
			c.setAccessible(true);
			address = TUnsafe.getUnsafe().allocateMemory(capacity);

			ByteBuffer instance = (ByteBuffer) c.newInstance(address, capacity);

			deallocator = new Deallocator(address, capacity);

			cleaner = Cleaner.create(this, deallocator);

			return instance;

		}catch(Exception e){
			Logger.error("Create ByteBufferChannel error. ", e);
			return null;
		}
	}

	/**
	 * 是否已经释放
	 * @return true 已释放, false: 未释放
	 */
	public boolean isReleased(){
		if(address == 0){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * 立刻释放内存
	 */
	public void release(){
		lock.lock();
		try{
			if(address != 0) {
				cleaner.clean();
				address = 0;
			}
		}finally {
			lock.unlock();
		}
	}

	private static class Deallocator implements Runnable {
		private long address;
		private int capacity;

		private Deallocator(long address, int capacity) {
			this.address = address;
			this.capacity = capacity;
		}

		public void setAddress(long address){
			this.address = address;
		}

		public void run() {

			if (this.address == 0) {
				return;
			}

			TUnsafe.getUnsafe().freeMemory(address);
			address = 0;
		}
	}

	/**
	 * 重新设置当前内存地址
	 */
	private void resetAddress(){
		lock.lock();
		try {
			this.address = TReflect.getFieldValue(byteBuffer, "address");
			deallocator.setAddress(address);
		}catch (ReflectiveOperationException e){
			Logger.error("ByteBufferChannel resetAddress() Error: "+e.getMessage(), e);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 当前数组空闲的大小
	 * @return 当前数组空闲的大小. -1: 已释放
	 */
	public int available(){
		if(isReleased()){
		    return -1;
		}

		return byteBuffer.capacity() - size;
	}

	/**
	 * 返回当前分配的容量
	 * @return 当前分配的容量. -1: 已释放
	 */
	public int capacity(){
		if(isReleased()){
		    return -1;
		}

		return byteBuffer.capacity();
	}

	/**
	 * 当前数据大小
	 * @return 数据大小 . -1: 已释放
	 */
	public int size(){
		if(isReleased()){
		    return -1;
		}

		return size;
	}

	/**
	 * 获取缓冲区有效字节数组的一个拷贝
	 *        修改这个数组将不会影响当前对象
	 *        返回 0 到 size 的有效数据
	 *        从堆外复制到堆内
	 * @return 缓冲区有效字节数组. null: 已释放
	 */
	public byte[] array(){
		if(isReleased()){
			throw new MemoryReleasedException("ByteBufferChannel is released.");
		}

		lock.lock();
		try {
			byte[] temp = new byte[size()];
			unsafe.copyMemory(null, address, temp, Unsafe.ARRAY_BYTE_BASE_OFFSET, size());
			return temp;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 清空通道
	 */
	public void clear(){
		if(isReleased()){
		    return;
		}

		lock.lock();
		try{
			byteBuffer.clear();
			size = 0;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 收缩通道内的数据
	 *
	 * @param shrinkSize 收缩的偏移量, 大于0,从尾部收缩数据,小于0 从头部收缩数据
	 * @return true: 成功, false: 失败
	 */
	public boolean shrink(int shrinkSize){
		if(isReleased()){
		    return false;
		}

		if(Math.abs(shrinkSize) > size){
			return true;
		}

		lock.lock();
		try{
            if(shrinkSize>0){
                size = size -  shrinkSize;
                byteBuffer.limit(size);
                return true;
            }else if(shrinkSize < 0 ){
                int position = byteBuffer.position();
                byteBuffer.position(shrinkSize * -1);
                if (TByteBuffer.moveData(byteBuffer, shrinkSize)) {
                    size = size + shrinkSize;
                    byteBuffer.limit(size);

                    int newPosition = position+shrinkSize;
                    newPosition = newPosition < 0 ? 0 : newPosition;
                    byteBuffer.position(newPosition);
                    return true;
                }else{
                    //收缩失败了,重置原 position 的位置
                    byteBuffer.position(position);
                    return false;
                }
            } else{
                return true;
            }
		} finally {
			lock.unlock();
		}
	}


	/**
	 * 获取某个偏移量位置的 byte 数据
	 *     该操作不会导致通道内的数据发生变化
	 * @param offset 偏移量
	 * @return byte 数据
	 */
	public byte get(int offset) throws IndexOutOfBoundsException {
		if(isReleased()){
			throw new MemoryReleasedException("ByteBufferChannel is released.");
		}

		if(offset >= 0 && offset <= size) {
			lock.lock();
			try {
				byte result = unsafe.getByte(address + offset);
				return result;
			} finally {
				lock.unlock();
			}
        } else {
            throw new IndexOutOfBoundsException();
        }
	}

	/**
	 * 获取某个偏移量位置的 byte 数据数组
	 *     该操作不会导致通道内的数据发生变化
	 * @param offset  偏移量
	 * @param dst     目标数组
	 * @param length  长度
	 * @return 获取数据的长度
	 */
	public int get(int offset, byte[] dst, int length) throws IndexOutOfBoundsException {
		if(isReleased()){
			throw new MemoryReleasedException("ByteBufferChannel is released.");
		}

		if(offset >= 0 && length <= size - offset) {
			lock.lock();
			try {
				int arrSize = length;

				if(length > size){
					arrSize = size;
				}

				unsafe.copyMemory(null, address + offset, dst, Unsafe.ARRAY_BYTE_BASE_OFFSET, length);

				return arrSize;
			} finally {
				lock.unlock();
			}
		} else {
			throw new IndexOutOfBoundsException();
		}
	}

	/**
	 * 获取某个偏移量位置的 byte 数据数组
	 *     该操作不会导致通道内的数据发生变化
	 * @param dst     目标数组
	 * @return 获取数据的长度
	 */
	public int get(byte[] dst){
		if(isReleased()){
			throw new MemoryReleasedException("ByteBufferChannel is released.");
		}

		lock.lock();
		try {
			int arrSize = dst.length;
			if(dst.length > size){
				arrSize = size;
			}

			unsafe.copyMemory(null, address, dst, Unsafe.ARRAY_BYTE_BASE_OFFSET, arrSize);

			return arrSize;

		}finally {
			lock.unlock();
		}
	}

	/**
	 * 获取缓冲区
	 *     返回 0 到 size 的有效数据
	 *	   为了保证数据一致性, 这里会加锁, 在调用getByteBuffer()方法后,所有读写操作都会被阻塞
	 *	   所以必须配合 compact() 方法使用,否则会导致锁死.
	 * @return ByteBuffer 对象
	 */
	public ByteBuffer getByteBuffer(){
		if(isReleased()){
			throw new MemoryReleasedException("ByteBufferChannel is released.");
		}

		lock.lock();
		return byteBuffer;
	}

	/**
	 * 收缩通道
	 *      将通过 getByteBuffer() 方法获得 ByteBuffer 对象的操作同步到 ByteBufferChannel
	 * 		如果之前最后一次通过 getByteBuffer() 方法获得过 ByteBuffer,则使用这个 ByteBuffer 来收缩通道
	 *      将 (position 到 limit) 之间的数据 移动到 (0  到 limit - position) 其他情形将不做任何操作
	 *		所以 建议 getByteBuffer() 和 compact() 成对操作
	 * @return 是否compact成功,true:成功, false:失败
	 */
	public boolean compact(){
		if(isReleased()){
		    return false;
		}

		if(!lock.isLocked()){
			lock.lock();
		}
		try{

			if(byteBuffer.position() == 0){
				return true;
			}

            int position = byteBuffer.position();
            boolean result = false;
            if(TByteBuffer.moveData(byteBuffer, position*-1)) {
                byteBuffer.position(0);
                size = size - position;
                byteBuffer.limit(size);

                result = true;
            }
			return result;

		} finally {
			if(lock.isLocked()) {
				lock.unlock();
			}
		}
	}

	/**
	 * 等待期望的数据长度
	 * @param length  期望的数据长度
	 * @param timeout 超时时间,单位: 秒
	 * @return true: 具备期望长度的数据, false: 等待数据超时
	 */
	public boolean waitData(int length,int timeout){
		while(timeout > 0){
			if(isReleased()){
				throw new MemoryReleasedException("ByteBufferChannel is released.");
			}

			if(size >= length){
				return true;
			}
			timeout -- ;
			TEnv.sleep(1);
		}
		return false;
	}


	/**
	 * 等待收到期望的数据
	 * @param mark  期望出现的数据
	 * @param timeout 超时时间,单位: 秒
	 * @return true: 具备期望长度的数据, false: 等待数据超时
	 */
	public boolean waitData(byte[] mark, int timeout){
		while(timeout > 0){
			if(isReleased()){
				throw new MemoryReleasedException("ByteBufferChannel is released.");
			}

			if(indexOf(mark) != -1){
				return true;
			}
			timeout -- ;
			TEnv.sleep(1);
		}
		return false;
	}

	/**
	 * 缓冲区头部写入
	 * @param src 需要写入的缓冲区 ByteBuffer 对象
	 * @return 写入的数据大小
	 */
	public int writeEnd(ByteBuffer src) {
		if(isReleased()){
			throw new MemoryReleasedException("ByteBufferChannel is released.");
		}

		if(src==null){
			return -1;
		}

		lock.lock();
		try {
			int writeSize = src.limit() - src.position();

			int limit = byteBuffer.limit();

			if (writeSize > 0) {
				//是否扩容
				if (available() < writeSize) {
					int newSize = byteBuffer.capacity() + writeSize;
					if (TByteBuffer.reallocate(byteBuffer, newSize)) {
						resetAddress();
					}
				}

			    int position = byteBuffer.position();
				byteBuffer.position(size);

				int old = byteBuffer.limit();

				size = size + writeSize;
				byteBuffer.limit(size);

				byteBuffer.put(src);

				byteBuffer.position(position);

			}

//			Logger.simple("W: " + writeSize + " \t" + limit + " \t" + byteBuffer.limit());
			return writeSize;

		} finally {
			lock.unlock();
		}
	}

	/**
	 * 缓冲区尾部写入
	 * @param src 需要写入的缓冲区 ByteBuffer 对象
	 * @return 读出的数据大小
	 */
	public int writeHead(ByteBuffer src) {
		if(isReleased()){
			throw new MemoryReleasedException("ByteBufferChannel is released.");
		}

		if (src == null) {
			 return -1;
		}

		lock.lock();
		try {

			byte[] srcByte = src.array();
			int writeSize = src.limit() - src.position();

			if (writeSize > 0) {
				//是否扩容
				if (available() < writeSize) {
					int newSize = byteBuffer.capacity() + writeSize;
					if (TByteBuffer.reallocate(byteBuffer, newSize)) {
						resetAddress();
					}
				}

				int position = byteBuffer.position();
				byteBuffer.position(0);

				//内容移动到 writeSize 之后
				if (TByteBuffer.moveData(byteBuffer, writeSize)) {

					byteBuffer.position(0);
					byteBuffer.put(src);

					size = size + writeSize;
					byteBuffer.limit(size);

					position = position + writeSize;
					position = position > size ? size : position;
					byteBuffer.position(position);
				}
			}

			return writeSize;

		} finally {
			lock.unlock();
		}

	}

	/**
	 * 从缓冲区头部读取数据
	 * @param dst 需要读入数据的缓冲区ByteBuffer 对象
	 * @return 读出的数据大小
	 */
	public int readHead(ByteBuffer dst) {
		if(isReleased()){
			throw new MemoryReleasedException("ByteBufferChannel is released.");
		}

		if(dst==null){
			return -1;
		}

		lock.lock();
	 	try {

			int readSize = 0;

			//确定读取大小
			if (dst.remaining() > size) {
				readSize = size;
			} else {
				readSize = dst.remaining();
			}

			int limit = byteBuffer.limit();

			if (readSize != 0) {
				int position = byteBuffer.position();
				byteBuffer.position(0);

				for (int i = 0; i < readSize; i++) {
					dst.put(byteBuffer.get());
				}

				if (TByteBuffer.moveData(byteBuffer, (readSize*-1))) {
					size = size - readSize;
					byteBuffer.limit(size);

					position = position+ (readSize*-1);
					position = position < 0 ? 0 : position;
					byteBuffer.position(position);
				} else {
					dst.reset();
				}
			}

			dst.flip();
			return readSize;

		} finally {
			lock.unlock();
		}

	}

	/**
	 * 从缓冲区尾部读取数据
	 * @param dst 需要读入数据的缓冲区ByteBuffer 对象
	 * @return 读出的数据大小
	 */
	public int readEnd(ByteBuffer dst) {
		if(isReleased()){
			throw new MemoryReleasedException("ByteBufferChannel is released.");
		}

		if(dst==null){
			return -1;
		}

		lock.lock();
		try {

			int readSize = 0;

			//确定读取大小
			if (dst.remaining() > size) {
				readSize = size;
			} else {
				readSize = dst.remaining();
			}

			if (readSize != 0) {
				int position = byteBuffer.position();

				byteBuffer.position(size - readSize);
				for (int i = 0; i < readSize; i++) {
					dst.put(byteBuffer.get());
				}
				size = size - readSize;
				byteBuffer.limit(size);

				position = position > size ? size : position;
				byteBuffer.position(position);
			}

			dst.flip();

			return readSize;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 查找特定 byte 标识的位置
	 *     byte 标识数组第一个字节的索引位置
	 * @param mark byte 标识数组
	 * @return 第一个字节的索引位置
	 */
	public int indexOf(byte[] mark){
		if(isReleased()){
			throw new MemoryReleasedException("ByteBufferChannel is released.");
		}

		if(size == 0){
			return -1;
		}

		int index = -1;
		byte[] tmp = new byte[mark.length];
		for(int offset = 0;offset <= size - mark.length; offset++){
            get(offset, tmp, tmp.length);
            if(Arrays.equals(mark, tmp)){
            	index = offset;
                break;
            }
		}

		return index;
	}

	/**
	 * 读取一行
	 * @return 字符串
	 */
	public String readLine() {
		if(isReleased()){
			throw new MemoryReleasedException("ByteBufferChannel is released.");
		}

		if(size == 0){
			return null;
		}

		String lineStr = "";
		int index = indexOf("\n".getBytes());

		if(index > 0) {
			byteBuffer.position(0);

			ByteBuffer lineBuffer = ByteBuffer.allocateDirect(index + 1);

			int readSize = readHead(lineBuffer);

			if (readSize == index + 1) {
				lineStr = TByteBuffer.toString(lineBuffer);
			}
			TByteBuffer.release(lineBuffer);
		}

		return lineStr.isEmpty()?null:lineStr;
	}


	/**
	 * 从 InputStream 读取一段,使用 byte数组 分割
	 * 		返回的 byte数组中不包含分割 byte 数组的内容
	 * @param splitByte 分割字节数组
	 * @return 字节数组
	 */
	public ByteBuffer readWithSplit(byte[] splitByte) {
		if(isReleased()){
			throw new MemoryReleasedException("ByteBufferChannel is released.");
		}

		int index = indexOf(splitByte);

		if(size == 0){
			return ByteBuffer.allocate(0);
		}

		if(index == 0){
			byteBuffer.position(splitByte.length);
			compact();
			index = indexOf(splitByte);
		}

		if(index == -1){
			index = size;
		}

		ByteBuffer resultBuffer = ByteBuffer.allocateDirect(index);
		int readSize = readHead(resultBuffer);
		TByteBuffer.release(resultBuffer);

		//跳过分割符
		shrink(splitByte.length*-1);
//		readHead(ByteBuffer.allocateDirect());

		return resultBuffer;
	}

	public void saveToFile(String filePath, long length) throws IOException{
		if(isReleased()){
			throw new MemoryReleasedException("ByteBufferChannel is released.");
		}

		int bufferSize = 1024*1024;

		if(length < bufferSize){
			bufferSize = Long.valueOf(length).intValue();
		}

		new File(TFile.getFileFolderPath(filePath)).mkdirs();

		RandomAccessFile randomAccessFile = null;
		File file = new File(filePath);
		byte[] buffer = new byte[bufferSize];
        try {
            randomAccessFile = new RandomAccessFile(file, "rwd");
			//追加形式
            randomAccessFile.seek(randomAccessFile.length());

            int loadSize = bufferSize;
            while(length > 0){
                loadSize = length > bufferSize ? bufferSize : new Long(length).intValue();
                byteBuffer.get(buffer, 0, loadSize);
                randomAccessFile.write(buffer, 0, loadSize);

                length = length - loadSize;
            }

            compact();
        }catch(IOException e){
            throw e;
        }finally {
            randomAccessFile.close();
        }
	}
}
