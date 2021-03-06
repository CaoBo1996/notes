# 索引

## 综述

页是``InnoDB``存储引擎管理存储空间的基本单位，一个页的大小是``16K``，也就是说``InnoDB``存储引擎一次将一页也就是``16K``的数据加载到内存，一次将一页数据刷新到磁盘上。而存放我们用户记录的页可以称之为数据页或者是索引页。

1. 用户记录的存储

   我们知道，在用户插入数据的时候，``InnoDB``存储引擎不但会把用户的实际的列数据记录下来，还会为每个列增加一些额外的头信息。也就是说，这些头信息加上用户插入的实际的列信息构成了整个用户的记录。这些头信息包含着很多重要的属性，包括``delete-mark``标志，即当我们在删除某条记录记录时，实际上底层并不是真正把这条记录给删除的，仅仅将该标志记为1，下次可能会空间重复复用。原因等会分析。``next_record``属性，这个属性特别重要，本质上这个属性就是一个指针，指向了下一条记录，即从当前记录的真实数据到下一条记录的真实数据的地址偏移量，本质上构成了一个链表的结构。即在插入新的数据的时候，直接插入即可。因为是一个链表的结构，所以插入的数据是非常快的。注意这个下一个记录不是我们插入的顺序，而是根据主键值的大小来决定的。因为我们知道，一条记录肯定是有一个主键值的。如果在创建表的时候，如果没有显示给出主键值，那么会自动帮我们创建一个隐藏列，``row_Id``，这个就是主键。``record_type``属性，这个属性也很重要当前记录的类型。最大记录和修小记录的``record_type``的值分别是2和3.我们知道对于一条。``n_owed``表示该组内此时又多少条记录。最小记录所在的组仅仅只能有一条记录，最大记录所在的组可以有1-8条记录，其他组只能由有4-8条记录。

   数据页与页之间本质上构成了一个双向链表，而单个数据页中中的记录本质上是按照主键的大小通过``next_record``属性，构成了一个单链表。并且每个数据页中有一个叫做目录的结构，实际上就是槽，每个槽对应着几条数据。因为数据存储是有序的，是按照主键值的大小排序的。所以我们在一个页中查找数据的时候，很容易通过二分法找到数据所在的槽，然后遍历槽中的所有的数据，找到目标记录。设置槽的原因是因为一个数据页的大小是``16k``，相比较于记录的大小来说还是挺大的，所以一个数据页中存放的数据量还是很多的，所以通过设计槽的数据结构，通过利用二分法思想，能大大加快搜索速度。并且页与页之间在物理存储上可能不直接相连，只要通过双向链表进行相关联即可。

2. 索引的作用
   1. 为什么要有索引
      1. 假设目前表中的记录非常少，所有的记录都可以放在一个页中，在查找记录的时候，可以根据搜索条件的不同分为两种情况：
         + 以主键为搜索条件
         由于页中的记录是按照主键的顺序进行排序的，并且数据与数据之间通过``next_record``形成了一个链表的结构，同时``mysql``还为页中的数据创建了槽的结构，每个槽对应着一定数量的记录，这样的话，我们可以根据二分法快速定位到待查询记录对应的槽，然后遍历该槽对应的记录分组中的记录即可很快的找到指定的记录。
         + 以其它列作为搜索条件
         由于数据页中没有为非主键列建立所谓的页目录，所以我们无法通过二分法快速定位到相应的槽，这种情况下只能从最小记录开始依次遍历单链表中的每条记录，然后对比每条记录是否符合搜索条件，很显然这种查询效率很低
      2. 在大多数的时候，我们表中存放的数据是非常多的，需要用多个数据页来存储这些数据，所以需要在多个页中查找相应的记录。在多个页中查找记录的话可以分为这两个步骤：
         + 定位到记录所在的页
         + 从所在的页中查找相应的记录
         + 在没有索引的前提下，不论是根据主键列或者其它的列的值进行查找，由于我们并不能快速的定位到记录所在的页，所以只能从第一个页沿着双向链表一直往下找，在每一个页中运用上述单页查找方法来查找指定的记录。所以非常耗时，效率低。

      3. 综合以上两点，我们需要设计一个结构，能快速帮我们提高查询速度。

   2. 设计简单的索引结构-索引设计思路
   在表中数据量很大，表中记录分布在多个页面的时候，我们查找一条数据，需要遍历所有的页面。在我们对多个页面进行查找数据的时候，我们之所以要遍历每一个页，是因为数据页中存放的数据是无特征的，是无序的，也是无规则的，那么如果我们可以给这些数据进行分类管理，会大大提高查询效率。首先，一个很自然的想法是按照主键的大小进行排序。从而形成了聚簇索引。我们需要保证下一个数据页用户数据的主键值必须全部大于上一个数据页用户数据的主键值，又因为一个数据页中的数据是按照主键值的大小进行排序的。我们必须要总是保持这个状态成立，所以我们必要通过记录移动等操作来保持这个状态成立。这个移动过程我们也可以称之为页分裂。这样受到启发，我们给每个页做一个类似于目录一样的东西。这个目录数据保存了这个页的页号和这个页存储的最小的主键的值。将这些目录全部都保存一个有序的结构，然后这些记录仍然要按照主键的大小进行排序，这样我们仍可以利用二分法寻找到特定的主键。既然有序，那么我们就可以使用二分法来快速的进行定位操作。这样，我们也把这个信息称之为记录，为了和之前的用户记录区分，我们称之为目录项记录。不过既然都是记录，那么我们页把他存在同一种类型的页中，即数据页。那么如何区分一个记录是用户记录还是目录记录。``record_type``。这样，我们无差别的对待用户记录和目录项记录。同样的存储在数据页中，同样的在页中建立页目录。同样的页中数据是以单项索引构成等。即除了一些标志位有差别外，其他的行为无差别。即索引即目录，目录即索引。

   聚簇索引：根据主键值的大小来进行叶子节点数据的排序，和非叶子节点目录记录的排序。即页内的记录按照主键值的大小排成一个单项链表
   二级索引：以上建立的索引本质上是对以主键作为搜索条件可以适用的。当搜索某条记录，不是以主键作为搜索条件的话，那么上述的``B+``树策略是失效的，所以我们需要额外建立另一种索引，也就是二级索引。也是一个``B+``树结构，不过叶子节点存放的是主键值和某个列的值，并且下一个页的所有的该列值均大于等于上一个页的所有的该列值。之所以会发生等于的原因是该列因为是非主键列，所以可能会发生重复。单个页中数据是按照该列的值从小到大顺序存放的，并且也为其建立一个槽结构，所以也很好解决查询的问题。而非叶子节点存放的是页号和该列的值。在最终查询时要进行一次回表的操作，即到聚簇索引中查到具体的记录，因为二级索引中的叶子节点没有存放完整的记录。
3. 为什么要提出区和段的概念

   1. 我们每向表中插入一条记录，本质上就是向该表的聚簇索引以及所有二级索引代表的``B+``树的节点中插入数据。而``B+``树的每一层中的页都会形成一个双向链表，如果是以页为单位来分配存储空间的话，双向链表相邻的两个页之间的物理位置可能离得非常远。我们介绍``B+``树索引的适用场景的时候特别提到范围查询只需要定位到最左边的记录和最右边的记录，然后沿着双向链表一直扫描就可以了，而如果链表中相邻的两个页物理位置离得非常远，就是所谓的随机``IO``。再一次强调，磁盘的速度和内存的速度差了好几个数量级，随机``IO``是非常慢的，所以我们应该尽量让链表中相邻的页的物理位置也相邻，这样进行范围查询的时候才可以使用所谓的顺序``IO``。即区是在磁盘中是直接存储在一起的，即**物体位置是连续的，即在磁盘上是连续的**。是一块连续的内存。

   2. 为什么要引入段的概念，首先要明确，段是一个**逻辑概念**，即段和区不一样，区对应着磁盘上的一个连续的存储空间，而段仅仅是一个物理概念。段可以说是由区和一些碎片页组成。把这些集合称之为一个段。存放叶子节点的区的集合就算是一个段（``segment``），存放非叶子节点的区的集合也算是一个段。如果不这样区别，那么当新申请的一个区，可能会有叶子节点页，非叶子节点页，等其他的各种页，而我们最终查找数据是在叶子节点中查找的，查找其实是对B+树叶子节点中的记录进行顺序扫描，如果有非叶子节点，那么又造成叶子节点存储的不连续了。所以一切都是为了``IO``次数，在此基础上以及减少随机IO的次数。**注意理解随机``IO``与顺序``IO``的区别**

   3. 对于``varchar``类型，``‘’``是不会占用存储空间的，在变长字段列表的值为00即可。对于``char（M）``类型，就算插入``‘’``,也会占内存，具体占用的内存根据采取的编码格式来确定，如何是可变编码格式，如``utf8``或者是``gbk``那，么会占据最小的内存，即是``M``,并且由于是可变编码，所以，也要写道变长字段列表中。如果是不可变编码，则直接占据``M*N``内存，且不用写到可变字符列表中
