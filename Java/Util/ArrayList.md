# ArrayList源码分析

``ArrayList``是一个动态数组，能够进行动态扩容。

+ ``ArrayList``重要属性

```java
private static final int DEFAULT_CAPACITY = 10;  // 默认初始化容量，即调用构造函数的时候，不指定任何参数，此时创建的数组容量大小为10
private static final Object[] EMPTY_ELEMENTDATA = {};  // 空数组，当调用构造函数时，传入的initCapacity为0时，会执行this.elementData = EMPTY_ELEMENTDATA。
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};  // 当调用构造函数时，若不指定任何参数，那么会执行this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
transient Object[] elementData;  // 内部数组对象的引用
private int size;  // 包含的元素的个数，初始化元素个数为0

```

+ ``ArrayList``的构造方法

```java
    /*
     强烈不建议传入initCapacity == 0。因为如果这样的话，后续添加元素的时候，会造成很多次的扩容操作。影响性能
     */
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
            //根据传入的initCapacity来创建一个Object[] elementData。即ArrayList内部实际上是一个数组
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            //一个空的数组对象 Object[] EMPTY_ELEMENTDATA = {}
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: " +
                    initialCapacity);
        }
    }

    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }

```

**Notes**：注意``DEFAULTCAPACITY_EMPTY_ELEMENTDATA``与``EMPTY_ELEMENTDATA``的区别。

   1. ``private static final Object[] EMPTY_ELEMENTDATA = {}``，而``private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {}``。即二者共同点都是一个空数组，但是二者用法不同。

   2. 若指定了``initCapacity``，那么如果该``initCapacity==0``，那么会创建一个空的``elementData``，即``this.elementData = EMPTY_ELEMENTDATA``

   3. 若没有指定``initCapacity``，即调用构造函数什么也不指定，那么也会创建一个空的``elementData``，但是此时是``this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA``

+ ``important method``分析
   1. ``add()``

   ```java
    public boolean add(E e) {
        //每次调用add()函数，都会执行ensureCapacityInternal()函数
        ensureCapacityInternal(size + 1);  // 传入参数为size + 1，表明此时需要数组最小的容量大小
        elementData[size++] = e;  // 将该元素添加到末尾。
        return true;
    }

    //当初始化时，元素个数肯定为0，当第一次执行add操作，此时需要的minCapacity是size+1，又此时size的值也为0，即此时minCapacity=1
    private void ensureCapacityInternal(int minCapacity) {
        /*
        说明：
        1.当elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA时，在第一次调用时，calculateCapacity的返回值为10。
        并且一直调用add()方法直到size+1 == 11时，才会之后每次返回size+1的值。
        2.否则，若elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA，那么直接返回minCapacity，即size+1
        */
        ensureExplicitCapacity(calculateCapacity(elementData, minCapacity));
    }

    /*
     该函数用来计算需要的数组空间的大小。主要是为了elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA服务的。
     如果不相等，那么直接就返回minCapacity，即size+1。否则，在相等的情况下，直到minCapacity >= 11，才会返回minCapacity。
     */
    private static int calculateCapacity(Object[] elementData, int minCapacity) {
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            //第一次调用时，由于minCapacity=1，所以此时若条件成立，那么返回值为DEFAULT_CAPACITY
            return Math.max(DEFAULT_CAPACITY, minCapacity);
        }
        return minCapacity; // 即size + 1
    }

    //此函数用来执行扩容操作。根据传入的minCapacity与当前elementData.length相比，来决定是否需要扩容。真正的扩容函数为 grow()。
    private void ensureExplicitCapacity(int minCapacity) {
        modCount++;
        /*
         判断此时需不需要扩容操作，判断依据：若minCapacity的值如果比elementData.length值大，则需要扩容，且每次扩容长度均为原来的1.5倍。注意：
         1.对于elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA和EMPTY_ELEMENTDATA这两种情况：
            1.1.第一次扩容，前者直接扩容为长度10的数组。然后只有在size+1的大于10之后才会进行第二次扩容。而后者第一次扩容只是扩容为1。当再次插入一个元素，仍然需要扩容。
         */
        if (minCapacity - elementData.length > 0)
            grow(minCapacity);
    }

    private void grow(int minCapacity) {
        int oldCapacity = elementData.length;
        /*
        容量增加为原来的1.5倍。注意当oldCapacity的值为0，或者1时，会出现newCapacity - minCapacity < 0
        首次扩容的情况下：有以下几个特例：
            1.当构造函数没有传入initCapacity参数，那么首次扩容会直接扩容为10。即第一个if语句成立。之后只有当
            size+1 >= 11，且mimCapacity == size + 1 > elementData>length时才会再次扩容，此时容量变为原来的1.5倍。
            2.当构造函数传入的参数，如果为0。那么第一次添加元素便会扩容。且扩容后容量为1。再次添加元素，仍会扩容，容量变为2。再次添加元素
            又会扩容，容量变为3。然后再次添加元素，仍然会扩容，容量变为4。再次添加元素，仍然会扩容，容量变为6。....可见，扩容操作太频繁。
            很影响性能。
        */

        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        //容操作需要调用 Arrays.copyOf() 把原数组整个复制到新数组中，
        elementData = Arrays.copyOf(elementData, newCapacity); 
    }

   ```

**Notes**：为了不影响性能，强烈建议在构造函数里不传入任何值。或者就算要传入值，最好传入更比较大一点的数。从而避免频繁扩容操作。影响性能。

