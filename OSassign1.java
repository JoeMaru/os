/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package OSassign1;

import java.util.Scanner;

public class OSassign1 {

    static int SIZE;                           //กำหนด size buffer
    static int REQUEST;                         //กำหนดจำนวน request
    static int CONSUMER;                        //กำหนดจำนวน consumer
    static int PRODUCER;                        //กำหนดจำนวน producer
    static long removeTimeout = 2;             //กำหนด timeout ของ function remove
    static long addTimeout = 2;                //กำหนด timeout ของ function append
    static int consumeNum = 0;              //ตัวแปรนับจำนวน consume
    static int produceNum = 0;              //ตัวแปรนับจำนวน produce
    static int num = 0;                     //ตัวแปรใช้นับการเรียก request 
    
 /*---------------------------main function--------------------------*/   
     public static void main(String[] args) throws InterruptedException {
        Scanner input = new Scanner(System.in);
        System.out.print("# buff ");
        PRODUCER = input.nextInt();
        CONSUMER = input.nextInt();
        SIZE = input.nextInt();
        REQUEST = input.nextInt();
        if(CONSUMER >  PRODUCER)         //เงื่อนไขสำหรับ check timeout ของ remove
               removeTimeout += (long)((4.3)*CONSUMER/ PRODUCER);   //เพิ่มค่า timeout ของ remove
        else if(CONSUMER <  PRODUCER)    //เงื่อนไขสำหรับ check timeout ของ append
                addTimeout += (long)((4.3)* PRODUCER/CONSUMER); //เพิ่มค่า timeout ของ append
        
        final PC pc = new PC(); //ประกาศ object PC ซึ่งสามารถทำการ produce และ consume wfh
        
        long tStart = System.currentTimeMillis(); //ตั้งค่าเวลาเริ่มต้น
        //วนสร้าง thread สำหรับ producer ตามจำนวนที่กำหนด
        Thread[] t1 = new Thread[PRODUCER];
        for (int i = 0; i < t1.length; i++) {
            t1[i] = new Thread(new Runnable() { //สร้าง producer thread สำหรับ run method append 
                public void run() {                 
                    
                    try {
                        pc.append();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            t1[i].start();  //ทำให้ producer thread อยู่ในฐานะพร้อม run 

        }
        //วนสร้าง thread สำหรับ consumer ตามจำนวนที่กำหนด
        Thread[] t2 = new Thread[CONSUMER];
        for (int i = 0; i < t2.length; i++) { //สร้าง producer thread สำหรับ run method remove
            t2[i] = new Thread(new Runnable() {
                public void run() {
                    
                    try {
                        pc.remove();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            t2[i].start(); //ทำให้ producer thread อยู่ในฐานะพร้อม run 

        }
        //สั่ง join thread เพื่อรอ producer thread ทำงานเสร็จ
        for (Thread thread : t1) { 
            thread.join();
        }
        //สั่ง join thread เพื่อรอ consumer thread ทำงานเสร็จ
        for (Thread thread : t2) {
            thread.join();
        }
        long tEnd = System.currentTimeMillis(); //เก็บค่าเวลาที่ทำงานเสร็จ
        long tDelta = tEnd - tStart;            //หาเวลาที่ใช้จาก เวลาที่เริ่ม-เวลาที่จบ ในหน่วย ms
        double elapsedSeconds = tDelta / 1000.0; //หาเวลาที่ใช้ในหน่วย s
        double throughput =  consumeNum/elapsedSeconds; //หา throughput
        System.out.println("\nProducer "+PRODUCER+", Consumers "+CONSUMER);
        System.out.println("Buffer size "+SIZE);
        System.out.println("Requests "+REQUEST); 
        System.out.print("\nSuccessfully consumed " + (consumeNum) + " requests (");
        System.out.printf("%.2f",(double)(consumeNum*100.0)/REQUEST);
         System.out.printf(" %%)\n");
        System.out.println("Elapsed time " + elapsedSeconds + "s");
        System.out.printf("Throughput %.2f successful request/s",throughput);
    }
/*----------------------------End main----------------------------*/
/*---------------------class circular buffer----------------------*/    
    public static class cirQ {      

        private int[] item = new int[SIZE];
        private int front = 0, rear = -1;
        private int count = 0;

        boolean isFull(cirQ q) {    //check buffer เต็ม
            return q.count == SIZE;
        }

        boolean isEmpty(cirQ q) {   //check buffer ว่าง
            return q.count == 0;
        }

        void add_item(cirQ q, int data) { // method สำหรับ add item ลงใน buffer
            q.count++;
            q.rear++;
            q.rear = q.rear % SIZE;
            q.item[q.rear] = data;
        }

        void remove_item(cirQ q) {  // method สำหรับ remove item ออกจาก buffer
            q.count--;
            q.front++;
            q.front = q.front % SIZE;
        }
    }
/*---------------------class PC----------------------*/
    public static class PC { 

        cirQ q = new cirQ();    //สร้าง object ของ circular buffer ซึ่งเป็น resource สำหรับ producer และ consumer 
        long timeout; //นับเวลา timeout สำหรับ wait
        
        public void append() throws InterruptedException { //method append สำหรับ producer thread
            int value = 0; //ตัวแปรสำหรับใส่ค่าใน buffer
            while (num < REQUEST) { //เข้า loop while เมื่อ producer thread ยังไม่เกิน request ที่กำหนด 

                
                timeout = System.currentTimeMillis() + addTimeout; //set เวลา time out ของ append
                
                synchronized (this) { // lock.acquire()
                    num++; //นับ request
                    
                    while (q.isFull(q) && num < REQUEST) {  // producer thread ทำการรอเมื่อ buffer เต็มและยังไม่เกิน request
                        if(num > REQUEST){ //ใช้ break ออกloopเมื่อเกิน request
                            break;
                        }
                        if(System.currentTimeMillis() > timeout){ //ถ้าเกิน timeout
                            timeout = System.currentTimeMillis() + addTimeout;
                            num++;
                        }
                      
                        wait(addTimeout); //รอ signal จาก consumer
                    } // lock.release()
                    
                    if(num > REQUEST){ //ถ้าเกิน request ให้ส่ง signal แจ้ง consumer แล้ว break ออกจาก loop
                        notify();
                        break;
                    }

                    q.add_item(q, value++); //insert ค่าใน circular buffer
                    
                    notify(); //ส่ง signal บอก consumer ว่าสามารถทำงานได้แล้ว

                } // lock.release()

            }
        }

        
        public void remove() throws InterruptedException { //method remove สำหรับ consumer thread
            while (true) {

                synchronized (this) { // lock.acquire()
                    
                    // consumer thread waits while list
                    // is empty
                    while (q.isEmpty(q) && num < REQUEST) { // consumer thread ทำการรอเมื่อ buffer เต็มและยังไม่เกิน request                   
                        wait(removeTimeout); //รอ signal จาก producer
                    }
                    
                    if(q.isEmpty(q) && num >= REQUEST){ //ถ้าbufferว่างและเกิน request ให้ส่ง signal แจ้ง producer แล้ว break ออกจาก loop
                        notify();
                        break;
                    }
                    
                   
                    q.remove_item(q); //remove ค่าใน circular buffer
                    consumeNum++;
                    
                    notify(); //ส่ง signal บอก producer ว่าสามารถทำงานได้แล้ว

                }   // lock.release()
            }
        }
    }
}

