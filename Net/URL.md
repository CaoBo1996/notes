# 一些小的点

1. ``URL``后面加``/``与不加``/``的区别
``http://www.example.com``和``http://www.example.com/``的区别。对于这种形式，基本上所有的主流浏览器都会在后面加上``/``，说明默认访问的是主机名为``WWW``的根目录。不然的话，浏览器也不知道访问哪里啊。访问 ``/``，本质上是访问根目录下的默认文件``index.html``。

2. 当``Web``服务器接收到对某个末尾不含斜杠的``url``请求时，例如``http://www.abc.com/abc``，这时服务器会搜索网站**根目录**下有没有名（全名，即包括了后缀名）为``abc``的文件，如果没有就**执行重定向操作**，把``abc``当做目录处理，然后返回``abc``目录下的**默认首页``index.html``**，即相当于发送了两次请求。当``Web``服务器接收到的是末尾带斜杠的请求时，``http://www.abc.com/abc/``，就会直接当做目录处理，然后返回``abc``目录下的**默认首页``index.html``**，即只发送了一次请求。

3. 网址没有加上``/``会给服务器增加一个查找是否有同名文件的过程。不``/``的``url``，请求的时候，会向服务器请求2次，同一时间数量过多的话，会给服务器造成压力，建议加上``/``。

4. 有``/``会认为是目录，没``/``会认为是文件。加了``/``浏览器会指向一个目录，目录的话会读取默认文件``index.html``，然后直接返回。没有``/``会先尝试读取文件，如果没有文件再找与该文件同名的目录，最后才读目录下的默认文件。注意基本主流的浏览器都会在``com``后面的会直接加上``/``。表示默认访问根目录的下默认文件``index.html``

5. 如果没有经过特别的配置，那么一个目录下的默认文件是``index.html``。

6. 内部应该进行一次重定向处理，即将不加``/``的重定向为加``/``。从而让这两个不同的``url``给用户返回相同的内容，从而不会造成用户混淆。重定向操作对于用户来说是不可见的。重定向相应的状态码为**3xx**。