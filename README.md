# qiniu-upload-plugins
Jenkins 的七牛插件,可以将构建好的结果上传到七牛.支持jekins2.0的pipline部署

### **配置**

 在全局配置里设置 App Key 和 Secret Key
 
![](https://raw.githubusercontent.com/shengxiluo20/qiniu-upload-plugins/master/src/main/images/global.png)

### **部署**

##### 1:普通部署      
选择要上传的文件和 bucket

![](https://raw.githubusercontent.com/shengxiluo20/qiniu-upload-plugins/master/src/main/images/config.png)


##### 2:pipline部署

```
stage('Upload') {
    steps {
        echo 'Upload'
        qiniu bucket: 'test', noUploadOnExists: true, noUploadOnFailure: true, source: '**/**', subKey: '/test', zone: 'zone1'
    }
}
```

### **结果**   
上传成功

![](https://raw.githubusercontent.com/shengxiluo20/qiniu-upload-plugins/master/src/main/images/result.jpg)