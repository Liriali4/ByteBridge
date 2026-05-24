package a;
public class T {
  public int m(int x, int y){
    int z = (x & y) ^ (x | y);
    int w = x > y ? x : y;
    return z + w;
  }
}
