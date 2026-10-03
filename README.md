<img src="https://github.com/LinrK114/Chick/blob/Android/app/src/main/assets/center_image.png" width="64" height="64">

# Chick 🐔 

# 描述 

- 点击发出鸡叫  

- 然后没啥其他的了  

- 你随便玩吧  

# 许可证 

本项目采用 [PolyForm Noncommercial License 1.0.0](LICENSE)（非商业许可证）授权  

- 个人、研究、教育和非营利用途免费  

- 允许使用、修改和分享  

- 商业使用需单独获得商业授权，请联系ASDFJ2023@outlook.com  

# 运行要求 

- Windows&ge;10

- .net10

# 编译 

- Windows:

```
dotnet publish Chick.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=false -p:WindowsPackageType=None -p:Platform=x64 -o D:\ChickPublish
```

- 这个编译命令输出程序默认在D:\ChickPublish\如需请修改  

# 其他分支 

- 这个分支为Windows其他分支分别为Android,Web如需请切换
