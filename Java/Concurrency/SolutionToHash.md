# 散列表

散列表是算法在**时间**和**空间**上做出权衡的例子。如果没有内存限制，我们可以直接将键（可能是一个很大的数）作为数组的索引，那么所有查找操作只要访问内存一次即可完成。如果没有时间的限制，那么只要使用无序数组即可。即直接将元素插入到数组中，然后进行顺序查找。这样就需要很少的内存。而散列表则使用了适度的时间和空间并在这两个极端之间找到了一种平衡。
使用散列的查找算法分为两步。第一步是用**散列函数**将被查找的键转换为数组的一个索引。在理想的情况下，不同的键会转换成不同的索引。当然这是理想的情况。所以我们要面对的是两个或者多个键被散列到相同的索引值的情况。所以散列查找的第二步就是处理``碰撞冲突``。两种经典的解决碰撞的方法：拉链法和线性探测法。

1. 散列函数

    + 作用：将键转换成数组的索引。如果我们有一个能保存``M``个键值对的数组，那么就需要一个能将任意键转化成该数组范围内的索引（``[0,M-1]``范围内的整数）的散列函数。我们所需要的散列函数要易于计算并且能够**均匀**分布所有的键，均匀的意思即对于任意键，0到``M-1``之间的每个整数都有相等的可能性与之相关（与键无关）。以下为一些类型的键以及可能的散列函数：
        1. 当键为正整数
        如果键是一个正整数，那么我们可以用``除留余数法``。我们可以选择一个大小为素数``M``的数组。**素数意味着除了1和它本身，没有其它的因数**。那么对于任意的正整数键``key``，计算``key``除以``M``的余数，这个余数作为这个键值对散列在数组中的元素的下标。从而我们能有效的将键散布在0-``M-1``之间。注意这个数组的大小不是一个素数，而是一个合数，那么可能无法均匀的散布这些``key``。例如，如果``M=10^k``，那么对于正整数键而言，我们仅仅能利用键的低``K``有效信息。这样的话，没有充分的利用整个键的信息，可能导致散列很不均匀。造成散列冲突严重。

        2. 浮点数
        如果``key``是一个浮点数，那么我可以将其乘以``M``并四舍五入得到一个介于0-``M-1``的数。此时不需要数组的大小``M``为一个素数。但是这个方法有一点缺陷，那就是键的高位数起的作用更大。最低位对结果可能都没有什么影响。解决的一个办法就是将键表示为二进制数，然后使用除留余数法。

        3. 字符串