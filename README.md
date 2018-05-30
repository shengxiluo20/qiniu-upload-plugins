# qiniu-upload-plugins
Jenkins 的七牛插件,可以将构建好的结果上传到七牛.支持jekins2.0的pipline部署

一:普通部署
1. 在全局配置里设置 App Key 和 Secret Key
![](https://gitee.com/shengxiluo/piplineScript/blob/master/global.png)
2. 选择要上传的文件和 bucket
![](https://gitee.com/shengxiluo/piplineScript/blob/master/config.png)


二:pipline部署

```
stage('Upload') {
    steps {
        echo 'Upload'
        qiniu bucket: 'test', noUploadOnExists: true, noUploadOnFailure: true, source: '**/**', subKey: '/test', zone: 'zone1'
    }
}
```
