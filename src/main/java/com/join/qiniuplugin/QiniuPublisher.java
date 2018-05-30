package com.join.qiniuplugin;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

/**
 * @description: 七牛上传主类
 * @author: chi
 * @Date: 18:32 2018/5/30/030
 */
public class QiniuPublisher extends Publisher implements SimpleBuildStep {

    private String source;
    private String bucket;
    private String zone;
    private String subKey;

    private boolean noUploadOnExists;
    private boolean noUploadOnFailure;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getSubKey() {
        return subKey;
    }

    public void setSubKey(String subKey) {
        this.subKey = subKey;
    }

    public boolean isNoUploadOnExists() {
        return noUploadOnExists;
    }

    public void setNoUploadOnExists(boolean noUploadOnExists) {
        this.noUploadOnExists = noUploadOnExists;
    }

    public boolean isNoUploadOnFailure() {
        return noUploadOnFailure;
    }

    public void setNoUploadOnFailure(boolean noUploadOnFailure) {
        this.noUploadOnFailure = noUploadOnFailure;
    }

    @DataBoundConstructor
    public QiniuPublisher(String source, String bucket, String zone, String subKey,
                          boolean noUploadOnFailure, boolean noUploadOnExists) {
        this.source = source;
        this.bucket = bucket;
        this.zone = zone;
        this.subKey = subKey;
        this.noUploadOnExists = noUploadOnExists;
        this.noUploadOnFailure = noUploadOnFailure;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath ws, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        String wsPath = ws.getRemote() + File.separator;
        Map<String, String> envVars = build.getEnvironment(listener);
        final boolean buildFailed = build.getResult() == Result.FAILURE;

        logger.println("开始上传到七牛...");

        //构造一个带指定Zone对象的配置类
        Configuration cfg;
        try {
            cfg = new Configuration((Zone) Zone.class.getDeclaredMethod(this.zone, null).invoke(null, null));
        } catch (Exception e) {
            logger.println("服务器Zone配置错误,跳过");
            return;
        }

        //...其他参数参考类注释
        UploadManager uploadManager = new UploadManager(cfg);

        Auth auth = Auth.create(this.getDescriptor().accessKey, this.getDescriptor().secretKey);

        String upToken;
        String expanded = Util.replaceMacro(this.source, envVars);

        logger.println(expanded);
        FilePath[] paths = ws.list(expanded);

        for (FilePath path : paths) {
            String keyPath = path.getRemote().replace(wsPath, "");
            String key = keyPath.replace(File.separator, "/");
            if (this.noUploadOnExists) {
                key = null;
            } else {
                if (this.subKey != null && !"".equals(this.subKey) && key.startsWith(this.subKey)) {
                    key = key.substring(key.indexOf(this.subKey) + this.subKey.length());
                }
            }

            try {
                upToken = auth.uploadToken(this.bucket);

                Response response = uploadManager.put(path.getRemote(), key, upToken);

                //解析上传成功的结果
                DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
                logger.println("以 " + this.zone + " 上传 " + keyPath + " 到 " + this.bucket + " 成功: " + putRet.key);
            } catch (QiniuException ex) {
                build.setResult(Result.UNSTABLE);
                Response r = ex.response;
                logger.println(r.toString());
                try {
                    logger.println(r.bodyString());
                } catch (QiniuException ex2) {
                    //ignore
                }
            }
        }
        logger.println("上传到七牛完成...");
    }

    /**
     * Descriptor for {@link QiniuPublisher}. Used as a singleton. The class is
     * marked as public so that it can be accessed from views.
     * <p>
     * <p>
     * See
     * <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Symbol("qiniu")
    @Extension
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Publisher> {

        private String accessKey, secretKey;

        public DescriptorImpl() {
            super(QiniuPublisher.class);
            load();
        }

        public FormValidation doCheckAccessKey(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Access Key 不能为空");
            return FormValidation.ok();
        }

        public FormValidation doCheckProfileName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("设置项名称不能为空");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project
            // types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "上传到七牛";
        }


        @Override
        public boolean configure(StaplerRequest req, JSONObject formData)
                throws FormException {
            req.bindParameters(this);
            this.accessKey = formData.getString("accessKey");
            this.secretKey = formData.getString("secretKey");
            save();
            return super.configure(req, formData);
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.STEP;
    }


}
