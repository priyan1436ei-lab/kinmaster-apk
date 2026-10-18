package dev.priyan.cut;
public class ClipTest {
    static void check(boolean b) { if(!b)throw new AssertionError(); }
    static void rejects(Runnable action) { try { action.run();throw new AssertionError("Should reject"); } catch(IllegalArgumentException expected){} }
    public static void main(String[] args) {
        Clip c=new Clip("content://test","test",10000);c.trim(1000,9000);
        Clip right=c.split(3000);check(c.inMs==1000&&c.outMs==4000&&right.inMs==4000&&right.outMs==9000);
        check(c.lengthMs()+right.lengthMs()==8000);
        rejects(()->c.trim(-1,10));rejects(()->c.trim(0,10001));rejects(()->c.trim(50,51));
        rejects(()->right.split(0));rejects(()->right.split(right.lengthMs()));
        Clip copy=right.copy();copy.text="changed";check(right.text.isEmpty());
        c.muted=true;c.rotation=90;c.grayscale=true;c.text="Hello";copy=c.copy();check(copy.muted&&copy.rotation==90&&copy.grayscale&&copy.text.equals("Hello"));
        rejects(()->new Clip("x","x",0));System.out.println("PASS: trim bounds, split conservation, invalid split, independent copy, effect retention, invalid duration");
    }
}
