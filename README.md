OpenNARS Declarative Core
-------------------------
![OpenNARS_Declarative_Core_Logo](https://user-images.githubusercontent.com/8284677/53459976-6ee8bb80-3a33-11e9-8ae4-2325fc587565.png)

Intended for people wanting to implement NAL7/8 in a different way.
Users, better go to https://github.com/opennars/opennars to enjoy a system that has full NAL1-8 implemented.

Source Code
-----------
In nars_core_java/ and nars_gui/ are the NARS core and the Swing GUI in Java. This came out of the code of Pei Wang in nars/ directory.

nars-dist/ contains a NARS distribution (executable, examples, web page with applet), that could be zipped for releases.

The test suite is nars_core/src/test/java/nars/main_nogui/TestReasoning0.java .

Build
-----
There are scripts for Linux and Windows to compile and create the executable jar:
build.sh and build.bat .

Test
----
The unit test suite is here. It ensures non-regression of the reasoner:
nars_core/src/test/java/nars/main_nogui/TestReasoning.java
It works classically: for each  XX-in.txt in directory nars-dist/Examples, it runs NARBatch, and compares actual result with reference result  XX-out.txt.
To create a new test input, add the NARS input as XX-in.txt in nars-dist/Examples , run the test suite, and move result file from temporary directory
/tmp/nars_test/XX-out.txt
into nars-dist/Example .
NOTE:
Due to the sensitivity of results regarding the implementation of the reasonner, it is difficult to write robust tests. But for pure non-regression tests, the test is usable.


Source Code status
------------------
See also http://code.google.com/p/open-nars/wiki/ProjectStatus
Current version has been fully tested for single capability at a time; there may still be bugs when combining capabilities.
Jean-Marc Vanel was working on this roadmap, mainly in GUI and software engineering tasks :
- reestablish a non-regression test suite
- make an independant syntax verifyer based on a grammar parser : it will give the column & line of error (there is a Scala combinator grammar)
- separe NARS in 2 modules with a Maven build : nars_gui and nars_core_java


mini nars 计划
------------------

正如王培老师所说, 一个学生 , 一个 NARS.

这是精简 nars 的又一次尝试.

有多精简? 可能会面目全非.

成功概率? 低于 10%.

具体事项:

- [ ] 尝试合并近似的类. 
- [ ] 修改继承关系.
- [ ] 简化术语.
- [ ] 将某些成员参数去除并转换成系统内部的 term 或 concept 或 node.
- [ ] 临时去除语言部分, 留待后天学习.
- [ ] 整合 jmonkey3D 或其它引擎.
- [ ] 完成短时回放/倒带功能.
- [ ] 实施以上精简的同时, 尽量保留 nars 的大部分层, 特别是第 9 层.
- [ ] 使用 mini nars 完成蚂蚁世界或乌鸦吃坚果实验.