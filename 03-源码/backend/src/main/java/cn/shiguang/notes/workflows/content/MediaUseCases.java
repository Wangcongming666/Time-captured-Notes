package cn.shiguang.notes.workflows.content;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import cn.shiguang.notes.modules.content.api.ContentApi;
import cn.shiguang.notes.platform.readmodel.PublicReadModel;
import cn.shiguang.notes.shared.contract.*;
import cn.shiguang.notes.shared.error.BizException;

/** Decodes before binding; filesystem names never depend on user-supplied paths. */
@Service
public class MediaUseCases {
    private final Path root;private final ContentApi content;private final PublicReadModel access;private final Semaphore slots=new Semaphore(4);
    public MediaUseCases(@Value("${app.media-root}")String root,ContentApi content,PublicReadModel access)throws IOException{this.root=Path.of(root).toAbsolutePath().normalize();Files.createDirectories(this.root);this.content=content;this.access=access;}
    @Transactional public Map<String,Object> upload(Actor actor,MultipartFile file,String purpose)throws IOException{
        actor.requireLogin();if(!Set.of("NOTE","PRACTICE","AVATAR").contains(purpose))throw BizException.invalid("无效图片用途");if(file.isEmpty()||file.getSize()>10*1024*1024)throw new BizException(413,"FILE_TOO_LARGE","图片必须在10MB以内");
        if(!slots.tryAcquire())throw new BizException(429,"IMAGE_BUSY","图片处理繁忙，请稍后重试");
        Path path=null;
        try(var input=ImageIO.createImageInputStream(file.getInputStream())){
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new BizException(415,"UNSUPPORTED_IMAGE","仅支持真实JPG、PNG或WebP图片");
            var reader=readers.next();BufferedImage image;String format;
            try{reader.setInput(input);format=reader.getFormatName().toLowerCase(Locale.ROOT);if(!Set.of("jpeg","jpg","png","webp").contains(format))throw new BizException(415,"UNSUPPORTED_IMAGE","图片格式不支持");int w=reader.getWidth(0),h=reader.getHeight(0);if((long)w*h>25_000_000)throw BizException.invalid("图片像素过大");image=reader.read(0);}finally{reader.dispose();}
            if(image==null)throw BizException.invalid("无法解码图片");var bytes=new ByteArrayOutputStream();String outFormat=format.equals("jpeg")||format.equals("jpg")?"jpg":"png";ImageIO.write(image,outFormat,bytes);byte[] data=bytes.toByteArray();if(data.length>10*1024*1024)throw new BizException(413,"FILE_TOO_LARGE","处理后的图片超过10MB");
            String key=UUID.randomUUID()+"."+outFormat;path=root.resolve(key);Path temp=Files.createTempFile(root,"upload-",".tmp");try{Files.write(temp,data);Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE);}finally{Files.deleteIfExists(temp);}
            final Path stored=path;TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(stored);}catch(IOException ignored){}}});
            return content.addMedia(actor,key,purpose,outFormat.equals("jpg")?"image/jpeg":"image/png",data.length,image.getWidth(),image.getHeight(),java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data)));
        }catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}catch(IOException|RuntimeException e){if(path!=null)Files.deleteIfExists(path);throw e;}finally{slots.release();}
    }
    public Map<String,Object> read(Actor actor,long id)throws IOException{var media=content.media(id);if(!access.mediaAllowed(actor,media))throw BizException.missing();Path path=root.resolve(Data.text(media,"storageKey")).normalize();if(!path.startsWith(root)||!Files.isRegularFile(path))throw BizException.missing();return Data.map("resource",new FileSystemResource(path),"mime",media.get("mime"));}
}
