package selo.to.aws.lambda;

import static java.lang.Math.max;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class AwsLambdaS3ImageScalerTest {

  private static final String SCALED_PREFIX = "scaled";
  private static final String EMPTY_IMAGE_PNG = "empty-image.png";
  private static final String BIG_IMAGE = "big-image.jpg";
  private static final String TEST_BUCKET = "seloto-images-test";
  private String key;

  @Before
  public void cleanup() {
    AmazonS3 s3Client = new AmazonS3Client();
    for (S3ObjectSummary s3ObjectSummary : s3Client.listObjects(TEST_BUCKET).getObjectSummaries()) {
      if (s3ObjectSummary.getKey().startsWith(SCALED_PREFIX)) {
        s3Client.deleteObject(TEST_BUCKET, s3ObjectSummary.getKey());
      }
    }
  }

  @Test
  public void scaleImageProduces3Sizes() throws Exception {
    givenImage(EMPTY_IMAGE_PNG);

    whenScalingImage();

    thenImageExists(AwsLambdaS3ImageScaler.PREFIX + "-" + 200 + "-" + EMPTY_IMAGE_PNG, 200);
    thenImageExists(AwsLambdaS3ImageScaler.PREFIX + "-" + 400 + "-" + EMPTY_IMAGE_PNG, 400);
    thenImageExists(AwsLambdaS3ImageScaler.PREFIX + "-" + 800 + "-" + EMPTY_IMAGE_PNG, 800);
  }

  @Test
  public void scaleBigImageWorks() throws Exception {
    givenImage(BIG_IMAGE);

    whenScalingImage();

    thenImageExists(AwsLambdaS3ImageScaler.PREFIX + "-" + 200 + "-" + BIG_IMAGE, 200);
    thenImageExists(AwsLambdaS3ImageScaler.PREFIX + "-" + 400 + "-" + BIG_IMAGE, 400);
    thenImageExists(AwsLambdaS3ImageScaler.PREFIX + "-" + 800 + "-" + BIG_IMAGE, 800);
  }

  private void thenImageExists(String key, int maxDimension) throws IOException {
    // Download the image from S3 into a stream
    AmazonS3 s3Client = new AmazonS3Client();
    S3Object s3Object = s3Client.getObject(new GetObjectRequest(TEST_BUCKET, key));
    InputStream objectData = s3Object.getObjectContent();

    // Read the source image
    BufferedImage image = ImageIO.read(objectData);
    int srcHeight = image.getHeight();
    int srcWidth = image.getWidth();

    assertThat(max(srcHeight, srcWidth), is(maxDimension));
  }

  private void whenScalingImage() throws IOException {
    AwsLambdaS3ImageScaler.scaleImage(TEST_BUCKET, key);
  }

  private void givenImage(String key) {
    this.key = key;
  }

}
