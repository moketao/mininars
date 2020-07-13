MiniNARS
-------------------------
![MiniNARS_logo](http://poerlang.com/mininars.png)

本项目源自 https://github.com/patham9/opennars_declarative_core , 一个以**通用人工智能**( [Artificial General Intelligence](http://www.agi-conf.org/) )为目标的项目, 由 [王培](https://cis.temple.edu/~pwang/) 老师发起.

当前项目( mini nars ) 只是测试项目, 要了解主干项目, 请访问 http://opennars.org

如何运行
-----------
1. 下载或克隆本项目.
1. 下载并安装 IntelliJ IDEA (社区版) https://www.jetbrains.com/idea/download/
1. 双击 mininars.iml, 点击 IDEA 编辑器右上角的绿色的运行按钮.


mini nars 计划
------------------

正如王培老师所说, 一个学生 , 一个 NARS.

由于 nars 给人的第印象是非常的复杂, 所以每位学生都试图将 nars 精简, 当前项目是又一次尝试.

有多精简? 可能会面目全非.

成功概率? 低于 10%. 

但即便如此, 也值得尝试, 只有这样才能对 nars 有更深的了解.

当然, 我还有别的目的:

1. 精简 nars 的数据结构和逻辑, 使其能在**显卡**中运行( nars 可以将其局部动态迁移至显卡, 类似慢思考和快思考的转换 ).
1. 精简 nars 以便于有更多的人能看懂 nars 的核心理论, 并以本项目为**跳板**, 为更深入的了解主干项目做准备.
1. 尝试制作一些图形化工具, 以便于人们能更**直观**的 "看见" nars 的所思所想, 为 AGI 的后天教育做准备.  

具体事项:

- [ ] 简化术语.
- [ ] 修改继承关系.
- [ ] 尝试合并近似的类. 
- [ ] 将某些成员参数去除并转换成系统内部的 term 或 concept 或 node.
- [ ] 临时去除语言部分, 留待后天学习.
- [x] 整合 jmonkey3D 为回放功能做准备.
- [ ] 完成短时回放/倒带功能.
- [ ] 实施以上精简的同时, 尽量保留 nars 的大部分层, 特别是第 9 层.
- [ ] 使用 mini nars 完成**蚂蚁世界**或**乌鸦吃坚果**实验.